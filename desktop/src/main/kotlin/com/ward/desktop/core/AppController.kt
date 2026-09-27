package com.ward.desktop.core

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.util.HttpUtil
import com.ward.desktop.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL

/** Состояние подключения - то, что рисует кнопка и трей. */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val since: Long) : ConnectionState
}

/** Сервер для списка: профиль и последний замер. */
data class ServerEntry(val guid: String, val profile: ProfileItem, val delayMillis: Long)

/** Группа серверов: подписка или ключи, добавленные вручную. */
data class ServerGroup(
    val id: String,
    val title: String,
    val subscription: SubscriptionCache?,
    val servers: List<ServerEntry>
)

/**
 * Всё состояние приложения и действия над ним. Интерфейс только читает потоки
 * и зовёт методы: сеть, диск и процесс ядра - здесь, вне главного потока.
 */
class AppController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val xray = XrayProcess()

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _groups = MutableStateFlow<List<ServerGroup>>(emptyList())
    val groups: StateFlow<List<ServerGroup>> = _groups.asStateFlow()

    private val _selected = MutableStateFlow(MmkvManager.getSelectServer())
    val selected: StateFlow<String?> = _selected.asStateFlow()

    /** Идёт долгая операция - обновление подписок или проверка серверов. */
    private val _busy = MutableStateFlow<String?>(null)
    val busy: StateFlow<String?> = _busy.asStateFlow()

    /** Сообщения для всплывающей плашки. */
    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    fun consumeMessage(msg: String) = _messages.update { it - msg }
    private fun say(msg: String) = _messages.update { it + msg }

    init {
        SystemProxy.recoverIfDirty()
        reload()
        // Подписки, которым пора обновиться, обновляем при запуске: фонового
        // планировщика у настольной версии нет, а запускают её каждый день
        scope.launch { updateDueSubscriptions() }
    }

    fun reload() {
        val subs = MmkvManager.decodeSubscriptions()
        val result = mutableListOf<ServerGroup>()
        subs.forEach { sub ->
            val servers = entries(MmkvManager.decodeServerList(sub.guid))
            // Пустая подписка по умолчанию - служебная, показывать её незачем
            if (sub.guid == AppConfig.DEFAULT_SUBSCRIPTION_ID && servers.isEmpty()) return@forEach
            result += ServerGroup(sub.guid, sub.subscription.remarks.ifBlank { "Подписка" }, sub, servers)
        }
        val standalone = entries(MmkvManager.decodeServerList(AppConfig.STANDALONE_SUBSCRIPTION_ID)) +
            if (subs.none { it.guid == AppConfig.DEFAULT_SUBSCRIPTION_ID })
                entries(MmkvManager.decodeServerList(AppConfig.DEFAULT_SUBSCRIPTION_ID)) else emptyList()
        if (standalone.isNotEmpty()) {
            result += ServerGroup(AppConfig.STANDALONE_SUBSCRIPTION_ID, "Мои ключи", null, standalone)
        }
        _groups.value = result
        _selected.value = MmkvManager.getSelectServer()
    }

    private fun entries(guids: List<String>): List<ServerEntry> = guids.mapNotNull { guid ->
        val profile = MmkvManager.decodeServerConfig(guid) ?: return@mapNotNull null
        ServerEntry(guid, profile, MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis ?: 0L)
    }

    // region Подключение

    fun toggle() {
        when (_state.value) {
            ConnectionState.Disconnected -> connect()
            else -> disconnect()
        }
    }

    fun connect() {
        val guid = MmkvManager.getSelectServer()
        if (guid.isNullOrBlank()) {
            say("Сначала выберите сервер")
            return
        }
        _state.value = ConnectionState.Connecting
        scope.launch {
            try {
                xray.start(guid) { code ->
                    // Ядро упало само - подключения больше нет, прокси убираем
                    SystemProxy.disable()
                    _state.value = ConnectionState.Disconnected
                    say("Ядро остановилось (код $code). Подробности в журнале")
                }
                val proxy = SystemProxy.enable(SettingsManager.getHttpPort(), SettingsManager.getSocksPort())
                if (!proxy.ok) say(proxy.message)
                _state.value = ConnectionState.Connected(System.currentTimeMillis())
            } catch (e: Exception) {
                AppLog.write('E', TAG, "connect failed", e)
                xray.stop()
                _state.value = ConnectionState.Disconnected
                say(e.message ?: "Не удалось подключиться")
            }
        }
    }

    fun disconnect() {
        scope.launch {
            SystemProxy.disable()
            xray.stop()
            _state.value = ConnectionState.Disconnected
        }
    }

    /** Выбор сервера. Если подключены - переподключаемся на новый. */
    fun select(guid: String) {
        if (guid == _selected.value) return
        MmkvManager.setSelectServer(guid)
        _selected.value = guid
        if (_state.value !is ConnectionState.Disconnected) {
            scope.launch {
                xray.stop()
                withContext(Dispatchers.Main) { connect() }
            }
        }
    }

    /** Вызывается при выходе: ядро и системный прокси не должны нас пережить. */
    fun shutdown() {
        SystemProxy.disable()
        xray.stop()
    }

    // endregion

    // region Подписки и ключи

    /** Добавить из текста: ссылка на подписку, ключ или несколько ключей. */
    fun import(text: String) {
        val input = text.trim()
        if (input.isEmpty()) return
        scope.launch {
            _busy.value = "Добавление…"
            try {
                val r = AngConfigManager.importBatchConfig(input, AppConfig.STANDALONE_SUBSCRIPTION_ID, true)
                when {
                    r.count + r.countSub == 0 -> say("Не удалось распознать ни ссылку, ни ключ")
                    r.hasSubFailures -> say("Подписка добавлена, но не скачалась. Попробуйте обновить её позже")
                    r.countSub > 0 -> say("Подписка добавлена")
                    else -> say("Добавлено серверов: ${r.count}")
                }
            } finally {
                _busy.value = null
                reload()
            }
        }
    }

    fun updateSubscription(id: String) {
        val sub = MmkvManager.decodeSubscription(id) ?: return
        scope.launch {
            _busy.value = "Обновление «${sub.remarks}»…"
            try {
                val r = AngConfigManager.updateConfigViaSub(SubscriptionCache(id, sub))
                if (r.failureCount > 0) say("Не удалось обновить «${sub.remarks}»")
            } finally {
                _busy.value = null
                reload()
            }
        }
    }

    fun updateAllSubscriptions() {
        scope.launch {
            _busy.value = "Обновление подписок…"
            try {
                val r = AngConfigManager.updateConfigViaSubAll()
                if (r.failureCount > 0) say("Не обновилось подписок: ${r.failureCount}")
            } finally {
                _busy.value = null
                reload()
            }
        }
    }

    private fun updateDueSubscriptions() {
        val now = System.currentTimeMillis()
        val due = MmkvManager.decodeSubscriptions().filter {
            val s = it.subscription
            s.enabled && s.autoUpdate && s.url.isNotBlank() &&
                now - s.lastUpdated > s.updateInterval * 60_000L
        }
        if (due.isEmpty()) return
        _busy.value = "Обновление подписок…"
        try {
            due.forEach { AngConfigManager.updateConfigViaSub(it) }
        } finally {
            _busy.value = null
            reload()
        }
    }

    fun deleteGroup(id: String) {
        scope.launch {
            if (id == AppConfig.STANDALONE_SUBSCRIPTION_ID) {
                MmkvManager.removeServerViaSubid(id)
                MmkvManager.removeServerViaSubid(AppConfig.DEFAULT_SUBSCRIPTION_ID)
            } else {
                SettingsManager.removeSubscriptionWithDefault(id)
            }
            reload()
        }
    }

    // endregion

    // region Проверка

    /**
     * Проверка серверов временем TCP-подключения. Параллельно, но не все сразу:
     * сотня одновременных подключений похожа на сканирование, и роутеры это не любят.
     */
    fun testServers(groupId: String? = null) {
        val targets = _groups.value.filter { groupId == null || it.id == groupId }.flatMap { it.servers }
        if (targets.isEmpty()) return
        scope.launch {
            _busy.value = "Проверка серверов…"
            val gate = Semaphore(16)
            try {
                targets.map { entry ->
                    async {
                        gate.withPermit {
                            val delay = tcpDelay(entry.profile)
                            MmkvManager.encodeServerTestDelayMillis(entry.guid, delay)
                        }
                    }
                }.awaitAll()
            } finally {
                _busy.value = null
                reload()
            }
        }
    }

    private fun tcpDelay(profile: ProfileItem): Long {
        val host = profile.server ?: return -1
        val port = profile.serverPort?.toIntOrNull() ?: return -1
        return try {
            val start = System.nanoTime()
            Socket().use { it.connect(InetSocketAddress(host, port), 3000) }
            ((System.nanoTime() - start) / 1_000_000).coerceAtLeast(1)
        } catch (_: Exception) {
            -1
        }
    }

    /** Проверка самого подключения: запрос через наш же прокси. */
    fun testConnection() {
        if (_state.value !is ConnectionState.Connected) return
        scope.launch {
            val url = SettingsManager.getDelayTestUrl()
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", SettingsManager.getHttpPort()))
            try {
                val start = System.nanoTime()
                val conn = URL(url).openConnection(proxy).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                conn.getInputStream().use { it.readBytes() }
                val ms = (System.nanoTime() - start) / 1_000_000
                say("Соединение работает: $ms мс")
            } catch (e: Exception) {
                say("Соединение не работает: ${e.message}")
            }
        }
    }

    // endregion

    private companion object {
        const val TAG = "AppController"
    }
}
