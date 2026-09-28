package com.v2ray.ang.service

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.RealPingEvent
import com.v2ray.ang.dto.SubscriptionUpdateMessage
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Обновление подписок - логика SubscriptionUpdateService с Android без службы и
 * уведомлений: обновить, при желании проверить серверы, убрать нерабочие и
 * отсортировать, затем сообщить экрану.
 */
object SubscriptionUpdateService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val updateSemaphore = Semaphore(2)

    fun handle(context: Context, message: SubscriptionUpdateMessage) {
        if (message.key != AppConfig.MSG_SUB_UPDATE_START) return
        scope.launch {
            updateSemaphore.withPermit {
                message.subIds.forEach { runCatching { updateSingle(context, it, message.forcedUpdate) }
                    .onFailure { e -> LogUtil.e(AppConfig.TAG, "SubscriptionUpdateService update failed", e) } }
            }
        }
    }

    private suspend fun updateSingle(context: Context, subId: String, forcedUpdate: Boolean) {
        val subItem = MmkvManager.decodeSubscription(subId) ?: return
        if (!subItem.enabled || subItem.url.isEmpty()) return
        val sub = SubscriptionCache(subId, subItem)

        if (forcedUpdate || MmkvManager.decodeSettingsBool(AppConfig.PREF_UPDATE_SUBSCRIPTION, false)) {
            AngConfigManager.updateConfigViaSub(sub)
        }
        if (MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_TEST_AFTER_UPDATE_SUBSCRIPTION, false)) {
            testServers(context, sub)
            if (MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_REMOVE_INVALID_AFTER_TEST, false)) {
                AngConfigManager.removeInvalidServer(subId)
            }
            if (MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_SORT_AFTER_TEST, false)) {
                AngConfigManager.sortByTestResultsForSub(subId)
            }
        }
        MessageHelper.sendMsg2UI(context, AppConfig.MSG_SUB_UPDATE_FINISH, subId)
    }

    private suspend fun testServers(context: Context, sub: SubscriptionCache) {
        val guids = MmkvManager.decodeServerList(sub.guid)
        if (guids.isEmpty()) return
        val done = CompletableDeferred<Unit>()
        RealPingWorkerService(context, guids) { event ->
            when (event) {
                is RealPingEvent.Result -> MmkvManager.encodeServerTestDelayMillis(event.guid, event.delayMillis)
                is RealPingEvent.Finish -> done.complete(Unit)
                else -> {}
            }
        }.start()
        done.await()
    }
}
