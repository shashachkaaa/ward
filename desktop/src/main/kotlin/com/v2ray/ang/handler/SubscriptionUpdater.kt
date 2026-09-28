package com.v2ray.ang.handler

import android.content.Context
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.SubscriptionUpdateMessage
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.util.LogUtil
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

/**
 * Автообновление подписок, настольная версия.
 *
 * На Android расписание ведёт WorkManager, и задача переживает перезапуск. Здесь
 * приложение и так живёт в трее: раз в несколько минут смотрим, каким подпискам
 * подошёл срок по их же интервалу, и обновляем их. Пропущенное за время, пока
 * приложение было закрыто, догоняется при первом же обходе.
 */
object SubscriptionUpdater {

    private var timer: Timer? = null
    private const val CHECK_PERIOD_MS = 5 * 60 * 1000L

    @Synchronized
    fun sync(context: Context = AngApplication.application, forceReschedule: Boolean = false) {
        if (timer == null) {
            timer = fixedRateTimer("sub-updater", daemon = true, initialDelay = 15_000L, period = CHECK_PERIOD_MS) {
                runCatching { runDue(context) }.onFailure { LogUtil.e(AppConfig.TAG, "SubscriptionUpdater tick failed", it) }
            }
        }
        LogUtil.i(AppConfig.TAG, "SubscriptionUpdater: sync complete forceReschedule=$forceReschedule")
    }

    fun syncOne(context: Context = AngApplication.application, subId: String) = sync(context)

    fun cancelOne(context: Context = AngApplication.application, subId: String) {}

    fun updateLastUpdatedAndReschedule(context: Context = AngApplication.application, subId: String) {
        val subItem = MmkvManager.decodeSubscription(subId) ?: return
        subItem.lastUpdated = System.currentTimeMillis()
        MmkvManager.encodeSubscription(subId, subItem)
    }

    private fun runDue(context: Context) {
        val now = System.currentTimeMillis()
        val due = MmkvManager.decodeSubscriptions().filter {
            val s = it.subscription
            val interval = maxOf(AppConfig.SUBSCRIPTION_MIN_INTERVAL_MINUTES, s.updateInterval) * 60_000L
            s.enabled && s.autoUpdate && s.url.isNotEmpty() && (s.lastUpdated <= 0 || now - s.lastUpdated >= interval)
        }.map { it.guid }
        if (due.isEmpty()) return
        due.forEach { updateLastUpdatedAndReschedule(context, it) }
        MessageHelper.sendMsg2SubscriptionService(
            context, SubscriptionUpdateMessage(AppConfig.MSG_SUB_UPDATE_START, true, due)
        )
    }
}
