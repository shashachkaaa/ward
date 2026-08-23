package com.v2ray.ang.handler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.extension.toSpeedString
import com.v2ray.ang.ui.compose.AccentPalette
import com.v2ray.ang.ui.main.MainActivity
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

object NotificationManager {
    private const val NOTIFICATION_ID = 1
    private const val NOTIFICATION_PENDING_INTENT_CONTENT = 0
    private const val NOTIFICATION_PENDING_INTENT_STOP_V2RAY = 1
    private const val NOTIFICATION_PENDING_INTENT_RESTART_V2RAY = 2
    private const val NOTIFICATION_ICON_THRESHOLD = 3000
    private const val QUERY_INTERVAL_MS = 3000L

    private var lastQueryTime = 0L
    private var mBuilder: NotificationCompat.Builder? = null
    private var speedNotificationJob: Job? = null
    private var mNotificationManager: NotificationManager? = null

    /**
     * Starts the speed notification.
     * @param currentConfig The current profile configuration.
     */
    fun startSpeedNotification() {
        // Опрос идёт всегда: с него живёт скорость на главном экране.
        // Настройка решает лишь, писать ли цифры в само уведомление
        if (speedNotificationJob != null || CoreServiceManager.isRunning() == false) return

        var lastZeroSpeed = false

        speedNotificationJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                lastZeroSpeed = updateSpeedNotificationOnce(lastZeroSpeed)
                delay(QUERY_INTERVAL_MS)
            }
        }
    }

    /**
     * Shows the notification.
     * @param currentConfig The current profile configuration.
     */
    fun showNotification(currentConfig: ProfileItem?) {
        val service = getService() ?: return

        // Reset last query time to avoid querying stats too soon after showing the notification
        lastQueryTime = System.currentTimeMillis()

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val startMainIntent = Intent(service, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(service, NOTIFICATION_PENDING_INTENT_CONTENT, startMainIntent, flags)

        val stopV2RayIntent = Intent(AppConfig.BROADCAST_ACTION_SERVICE)
        stopV2RayIntent.`package` = AppConfig.ANG_PACKAGE
        stopV2RayIntent.putExtra("key", AppConfig.MSG_STATE_STOP)
        val stopV2RayPendingIntent = PendingIntent.getBroadcast(service, NOTIFICATION_PENDING_INTENT_STOP_V2RAY, stopV2RayIntent, flags)

        val restartV2RayIntent = Intent(AppConfig.BROADCAST_ACTION_SERVICE)
        restartV2RayIntent.`package` = AppConfig.ANG_PACKAGE
        restartV2RayIntent.putExtra("key", AppConfig.MSG_STATE_RESTART)
        val restartV2RayPendingIntent = PendingIntent.getBroadcast(service, NOTIFICATION_PENDING_INTENT_RESTART_V2RAY, restartV2RayIntent, flags)

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        mBuilder = NotificationCompat.Builder(service, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(currentConfig?.remarks ?: service.getString(R.string.app_name))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            // Акцент из настроек. Заливка им всего уведомления обязательна, а не
            // на вкус: без неё система не считает уведомление годным для «живого»
            // (hasPromotableCharacteristics требует isColorizedRequested), и плашки
            // не будет. За читаемость текста на цветном фоне отвечает сама система:
            // она подбирает контраст, а не кладёт белым поверх чего попало.
            // Работает это только у службы переднего плана - наш случай
            .setColor(accentColor())
            .setColorized(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_delete_24dp,
                service.getString(R.string.notification_action_stop_v2ray),
                stopV2RayPendingIntent
            )
            .addAction(
                R.drawable.ic_restore_24dp,
                service.getString(R.string.title_service_restart),
                restartV2RayPendingIntent
            )

        //mBuilder?.setDefaults(NotificationCompat.FLAG_ONLY_ALERT_ONCE)

        applyLiveUpdate(service.getString(R.string.notification_live_connected))

        val notification = mBuilder?.build()
        if (notification != null && liveUpdateEnabled()) {
            // Система умеет сказать заранее, годится ли уведомление в «живые».
            // Без этого пришлось бы гадать, почему плашки нет: не показала оболочка
            // или мы сами собрали уведомление не так
            LogUtil.e(
                AppConfig.TAG,
                "Live notification: promotable=" +
                        NotificationCompat.hasPromotableCharacteristics(notification)
            )
        }

        service.startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * Просит систему поднять уведомление до «живого».
     *
     * С Android 16 такое уведомление система выносит отдельной плашкой в строку
     * состояния и на экран блокировки - там видно короткую строку, скорость в нашем
     * случае. Просьба именно просьба: система вправе отказать, и тогда останется
     * обычное уведомление службы. На версиях до 16 вызовы просто ничего не делают.
     *
     * Оболочки вроде OriginOS или HyperOS рисуют такие уведомления по-своему -
     * «островом» или «фокусом». Подхватят ли они стандартную просьбу, зависит от
     * оболочки, и узнать это можно только на живом устройстве.
     *
     * @param shortText Короткая строка для плашки. Место там на несколько символов.
     */
    private fun applyLiveUpdate(shortText: String) {
        val builder = mBuilder ?: return

        // Выключили при поднятом туннеле - плашку надо убрать сразу, а не ждать
        // переподключения: просьба уже отправлена и сама собой не отзовётся
        if (!liveUpdateEnabled()) {
            builder.setRequestPromotedOngoing(false)
            return
        }

        builder.setRequestPromotedOngoing(true)
        builder.setShortCriticalText(shortText)
    }

    /** Просить ли систему о живом уведомлении: есть ли куда просить и хотят ли этого. */
    private fun liveUpdateEnabled(): Boolean =
        Build.VERSION.SDK_INT >= 36 &&
                MmkvManager.decodeSettingsBool(AppConfig.PREF_LIVE_NOTIFICATION, true)

    /** Цвет акцента из настроек - тот же, которым покрашено приложение. */
    private fun accentColor(): Int =
        AccentPalette.find(MmkvManager.decodeSettingsString(AppConfig.PREF_ACCENT_COLOR))
            .seed
            .toArgb()

    /**
     * Fulfills or refreshes the foreground-service contract before a start command can
     * return early. A duplicate startForegroundService call still requires the service
     * to enter foreground state promptly, even when the core is already running.
     */
    fun ensureForeground() {
        val service = getService() ?: return
        val notification = mBuilder?.build()
        if (notification == null) showNotification(null) else service.startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * Cancels the notification.
     */
    fun cancelNotification() {
        val service = getService() ?: return
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)

        mBuilder = null
        speedNotificationJob?.cancel()
        speedNotificationJob = null
        mNotificationManager = null
        TrafficSpeedState.reset()
    }

    /**
     * Stops the speed notification.
     */
    fun stopSpeedNotification() {
        speedNotificationJob?.let {
            it.cancel()
            speedNotificationJob = null
            updateNotification("", null, 0, 0)
            TrafficSpeedState.reset()
        }
    }

    /**
     * Creates a notification channel for Android O and above.
     * @return The channel ID.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = AppConfig.RAY_NG_CHANNEL_ID
        val channelName = AppConfig.RAY_NG_CHANNEL_NAME
        // Foreground-service notifications must remain visible; LOW is silent but valid.
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        chan.lightColor = Color.DKGRAY
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        getNotificationManager()?.createNotificationChannel(chan)
        return channelId
    }

    /**
     * Updates the notification with the given content text and traffic data.
     * @param contentText The content text.
     * @param proxyTraffic The proxy traffic.
     * @param directTraffic The direct traffic.
     */
    private fun updateNotification(
        contentText: String?,
        shortText: String?,
        proxyTraffic: Long,
        directTraffic: Long
    ) {
        val builder = mBuilder ?: return

        if (proxyTraffic < NOTIFICATION_ICON_THRESHOLD && directTraffic < NOTIFICATION_ICON_THRESHOLD) {
            builder.setSmallIcon(R.drawable.ic_stat_name)
        } else if (proxyTraffic > directTraffic) {
            builder.setSmallIcon(R.drawable.ic_stat_proxy)
        } else {
            builder.setSmallIcon(R.drawable.ic_stat_direct)
        }

        // Строку уведомления обновляем только если её показывают. Плашка живёт своей
        // жизнью: она нужна и тогда, когда цифры скорости в уведомлении выключены
        if (contentText != null) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            builder.setContentText(contentText)
        }
        if (shortText != null) {
            applyLiveUpdate(shortText)
        }

        getNotificationManager()?.notify(NOTIFICATION_ID, builder.build())
    }

    /**
     * Gets the notification manager.
     * @return The notification manager.
     */
    private fun getNotificationManager(): NotificationManager? {
        if (mNotificationManager == null) {
            val service = getService() ?: return null
            mNotificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return mNotificationManager
    }

    /**
     * Appends the speed string to the given text.
     * @param text The text to append to.
     * @param name The name of the tag.
     * @param up The uplink speed.
     * @param down The downlink speed.
     */
    private fun appendSpeedString(text: StringBuilder, name: String?, up: Double, down: Double) {
        var n = name ?: "no tag"
        n = n.take(min(n.length, 6))
        text.append(n)
        for (i in n.length..6 step 2) {
            text.append("\t")
        }
        text.append("•  ${up.toLong().toSpeedString()}↑  ${down.toLong().toSpeedString()}↓\n")
    }

    /**
     * Updates the speed notification once.
     * Queries traffic stats, separates proxy and direct, and updates the notification.
     * @param lastZeroSpeed The previous zero speed state.
     * @return The current zero speed state.
     */
    private fun updateSpeedNotificationOnce(lastZeroSpeed: Boolean): Boolean {
        val queryTime = System.currentTimeMillis()
        val sinceLastQueryIn = (queryTime - lastQueryTime)

        // If the query interval is too short, skip this round to avoid excessive CPU usage
        if (sinceLastQueryIn < QUERY_INTERVAL_MS) {
            LogUtil.w(AppConfig.TAG, "Query interval too short: ${sinceLastQueryIn}ms, skipping")
            lastQueryTime = queryTime
            return lastZeroSpeed
        }
        val sinceLastQueryInSeconds = sinceLastQueryIn / 1000.0

        var proxyUplink = 0L
        var proxyDownlink = 0L
        var directUplink = 0L
        var directDownlink = 0L
        var otherUplink = 0L
        var otherDownlink = 0L

        CoreServiceManager.queryAllOutboundTrafficStats().forEach { stat ->
            when {
                stat.tag == AppConfig.TAG_DIRECT -> {
                    when (stat.direction) {
                        AppConfig.UPLINK -> directUplink += stat.value
                        AppConfig.DOWNLINK -> directDownlink += stat.value
                    }
                }

                stat.tag.startsWith(AppConfig.TAG_PROXY) -> {
                    when (stat.direction) {
                        AppConfig.UPLINK -> proxyUplink += stat.value
                        AppConfig.DOWNLINK -> proxyDownlink += stat.value
                    }
                }

                // Готовые конфиги называют исходящие как угодно - без этой ветки
                // их трафик не попадал бы ни в одну корзину и терялся
                else -> {
                    when (stat.direction) {
                        AppConfig.UPLINK -> otherUplink += stat.value
                        AppConfig.DOWNLINK -> otherDownlink += stat.value
                    }
                }
            }
        }

        val proxyTotal = proxyUplink + proxyDownlink
        val directTotal = directUplink + directDownlink
        val otherTotal = otherUplink + otherDownlink
        val zeroSpeed = proxyTotal + directTotal + otherTotal == 0L

        // Тот же замер отдаём интерфейсу: счётчики ядра приходят с обнулением,
        // так что опрашивать их вторым циклом ради главного экрана нельзя
        // Ядро живёт в отдельном процессе, поэтому замер уходит интерфейсу сообщением:
        // общий объект между процессами не разделяется, и на экране были бы нули
        getService()?.let { service ->
            val speed = TrafficSpeed(
                proxyUp = (proxyUplink / sinceLastQueryInSeconds).toLong(),
                proxyDown = (proxyDownlink / sinceLastQueryInSeconds).toLong(),
                directUp = (directUplink / sinceLastQueryInSeconds).toLong(),
                directDown = (directDownlink / sinceLastQueryInSeconds).toLong(),
                otherUp = (otherUplink / sinceLastQueryInSeconds).toLong(),
                otherDown = (otherDownlink / sinceLastQueryInSeconds).toLong()
            )
            MessageHelper.sendMsg2UI(
                service,
                AppConfig.MSG_TRAFFIC_SPEED,
                TrafficSpeedState.encode(speed, sinceLastQueryInSeconds)
            )
        }

        if (!zeroSpeed || !lastZeroSpeed) {
            val text = StringBuilder()
            appendSpeedString(
                text, AppConfig.TAG_PROXY,
                proxyUplink / sinceLastQueryInSeconds,
                proxyDownlink / sinceLastQueryInSeconds
            )

            appendSpeedString(
                text, AppConfig.TAG_DIRECT,
                directUplink / sinceLastQueryInSeconds,
                directDownlink / sinceLastQueryInSeconds
            )

            val speedShown = MmkvManager.decodeSettingsBool(AppConfig.PREF_SPEED_ENABLED) == true
            // В плашку идёт входящая скорость: места там на несколько символов, а из
            // всех цифр эта - та самая, ради которой на неё и посмотрят
            val shortText = ((proxyDownlink + directDownlink + otherDownlink) /
                    sinceLastQueryInSeconds).toLong().toSpeedString()

            if (speedShown || liveUpdateEnabled()) {
                updateNotification(
                    contentText = text.toString().takeIf { speedShown },
                    shortText = shortText,
                    proxyTraffic = proxyTotal,
                    directTraffic = directTotal
                )
            }
        }
        lastQueryTime = queryTime
        return zeroSpeed
    }

    /**
     * Gets the service instance.
     * @return The service instance.
     */
    private fun getService(): Service? {
        return CoreServiceManager.serviceControl?.get()?.getService()
    }
}
