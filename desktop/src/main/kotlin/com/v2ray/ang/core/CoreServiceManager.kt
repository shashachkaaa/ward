package com.v2ray.ang.core

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.OutboundTrafficStat
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.handler.TrafficSpeed
import com.v2ray.ang.handler.TrafficSpeedState
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.util.LogUtil
import com.ward.desktop.core.SystemProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import libv2ray.CoreCallbackHandler
import java.io.Serializable

/**
 * Служба ядра, настольная версия.
 *
 * На Android это VpnService или служба прокси в отдельном процессе, с которой
 * интерфейс говорит сообщениями. Здесь процесс один, но разговор тот же: те же
 * сообщения MSG_* и те же ответы, поэтому экраны скопированы без правок.
 * Подключение - это ядро xray дочерним процессом плюс системный прокси.
 */
object CoreServiceManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val coreController = CoreNativeManager.newCoreController(CoreCallback)
    private var currentConfig: ProfileItem? = null
    private var speedJob: Job? = null

    fun isRunning() = coreController.isRunning

    fun getRunningServerName() = currentConfig?.remarks.orEmpty()

    /** Подключиться к выбранному серверу. Ответ приходит интерфейсу сообщением. */
    fun start(context: Context) {
        scope.launch {
            try {
                doStart(context)
                MessageHelper.sendMsg2UI(context, AppConfig.MSG_STATE_START_SUCCESS, "")
            } catch (e: Exception) {
                val message = e.message?.takeUnless { it.isBlank() } ?: e.javaClass.simpleName
                LogUtil.e(AppConfig.TAG, "StartCore-Manager: $message", e)
                runCatching { coreController.stopLoop() }
                SystemProxy.disable()
                MessageHelper.sendMsg2UI(context, AppConfig.MSG_STATE_START_FAILURE, message)
            }
        }
    }

    @Synchronized
    private fun doStart(context: Context) {
        val guid = MmkvManager.getSelectServer() ?: error("No server selected")
        val config = MmkvManager.decodeServerConfig(guid) ?: error("Failed to decode server config")
        LogUtil.i(AppConfig.TAG, "StartCore-Manager: Starting core loop for ${config.remarks}")

        // geo-файлы из установки - в папку, откуда их читает ядро
        SettingsManager.initAssets(context, context.assets)
        CoreNativeManager.initCoreEnv(context)

        val result = CoreConfigManager.getV2rayConfig(context, guid)
        if (!result.status) error(result.errorMessage.ifBlank { "Failed to get V2Ray config" })

        currentConfig = config
        coreController.startLoop(result.content, 0)
        if (!isRunning()) error("Core failed to start")

        val proxy = SystemProxy.enable(SettingsManager.getHttpPort(), SettingsManager.getSocksPort())
        if (!proxy.ok) com.ward.desktop.DesktopUi.toast(proxy.message)
        startSpeedLoop(context)
        LogUtil.i(AppConfig.TAG, "StartCore-Manager: Core started successfully")
    }

    /** Отключиться. Системный прокси возвращается до остановки ядра - чтобы не было окна без сети. */
    fun stop(context: Context) {
        scope.launch {
            stopBlocking()
            MessageHelper.sendMsg2UI(context, AppConfig.MSG_STATE_STOP_SUCCESS, "")
        }
    }

    /** Остановка без сообщений - для выхода из приложения. */
    @Synchronized
    fun stopBlocking() {
        speedJob?.cancel()
        speedJob = null
        SystemProxy.disable()
        runCatching { coreController.stopLoop() }
            .onFailure { LogUtil.e(AppConfig.TAG, "StartCore-Manager: Failed to stop core", it) }
        TrafficSpeedState.reset()
    }

    /** Сообщения, которые на Android получает служба. */
    fun onServiceMessage(context: Context, what: Int, content: Serializable) {
        when (what) {
            AppConfig.MSG_REGISTER_CLIENT -> MessageHelper.sendMsg2UI(
                context,
                if (isRunning()) AppConfig.MSG_STATE_RUNNING else AppConfig.MSG_STATE_NOT_RUNNING,
                ""
            )
            AppConfig.MSG_STATE_STOP -> stop(context)
            AppConfig.MSG_STATE_RESTART -> scope.launch {
                stopBlocking()
                start(context)
            }
            AppConfig.MSG_MEASURE_DELAY -> measureDelay(context)
        }
    }

    fun queryAllOutboundTrafficStats(): List<OutboundTrafficStat> {
        if (!isRunning()) return emptyList()
        return coreController.queryAllOutboundTrafficStats().split(';').mapNotNull { entry ->
            val parts = entry.split(',', limit = 3)
            if (parts.size != 3) return@mapNotNull null
            OutboundTrafficStat(tag = parts[0], direction = parts[1], value = parts[2].toLongOrNull() ?: return@mapNotNull null)
        }
    }

    private fun measureDelay(context: Context) {
        if (!isRunning()) return
        scope.launch {
            var time = -1L
            var error = ""
            for (second in listOf(false, true)) {
                try {
                    time = coreController.measureDelay(SettingsManager.getDelayTestUrl(second))
                    break
                } catch (e: Exception) {
                    error = e.message.orEmpty()
                }
            }
            val text = if (time >= 0) {
                context.getString(R.string.connection_test_available, time)
            } else {
                context.getString(R.string.connection_test_error, humanize(context, error))
            }
            MessageHelper.sendMsg2UI(context, AppConfig.MSG_MEASURE_DELAY_SUCCESS, text)
        }
    }

    private fun humanize(context: Context, raw: String): String = when {
        raw.contains("timed out", true) || raw.contains("timeout", true) -> context.getString(R.string.connection_test_timeout)
        raw.contains("refused", true) -> context.getString(R.string.connection_test_refused)
        raw.contains("unknown host", true) || raw.contains("dns", true) -> context.getString(R.string.connection_test_dns)
        raw.isBlank() -> context.getString(R.string.connection_test_unknown)
        else -> raw
    }

    /**
     * Скорость раз в секунду - как у уведомления на Android: счётчики ядра со
     * сбросом, разложенные на прокси, напрямую и прочее, уходят экрану сообщением.
     */
    private fun startSpeedLoop(context: Context) {
        speedJob?.cancel()
        speedJob = scope.launch {
            var last = System.currentTimeMillis()
            while (isActive && isRunning()) {
                delay(1000)
                val now = System.currentTimeMillis()
                val seconds = (now - last) / 1000.0
                last = now
                var pu = 0L; var pd = 0L; var du = 0L; var dd = 0L; var ou = 0L; var od = 0L
                queryAllOutboundTrafficStats().forEach { s ->
                    val up = s.direction == AppConfig.UPLINK
                    when {
                        s.tag == AppConfig.TAG_DIRECT -> if (up) du += s.value else dd += s.value
                        s.tag.startsWith(AppConfig.TAG_PROXY) -> if (up) pu += s.value else pd += s.value
                        s.tag == "ward-api" -> {}
                        else -> if (up) ou += s.value else od += s.value
                    }
                }
                val speed = TrafficSpeed(
                    proxyUp = (pu / seconds).toLong(), proxyDown = (pd / seconds).toLong(),
                    directUp = (du / seconds).toLong(), directDown = (dd / seconds).toLong(),
                    otherUp = (ou / seconds).toLong(), otherDown = (od / seconds).toLong()
                )
                MessageHelper.sendMsg2UI(context, AppConfig.MSG_TRAFFIC_SPEED, TrafficSpeedState.encode(speed, seconds))
            }
        }
    }

    /** Ядро упало само: подключения больше нет, прокси убираем и говорим экрану. */
    private object CoreCallback : CoreCallbackHandler {
        override fun startup(): Long = 0
        override fun shutdown(): Long {
            SystemProxy.disable()
            speedJob?.cancel()
            TrafficSpeedState.reset()
            MessageHelper.sendMsg2UI(android.content.Context.app, AppConfig.MSG_STATE_STOP_SUCCESS, "")
            return 0
        }
        override fun onEmitStatus(code: Long, message: String?): Long = 0
    }
}
