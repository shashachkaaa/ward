package com.v2ray.ang.handler

import android.app.NotificationManager as SystemNotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil

/**
 * Почему нет «живого» уведомления.
 *
 * Условий у системы несколько, и молчит она обо всех сразу: плашки просто нет.
 * Гадать по её отсутствию бесполезно - тут каждое условие спрашивается отдельно и
 * показывается в настройках. Журнал для этого не годится: на части прошивок его
 * из приложения не прочитать.
 */
enum class LiveNotificationStatus {

    /** Живых уведомлений не бывает: система старше Android 16. */
    UNSUPPORTED,

    /** Выключено в настройках приложения. */
    DISABLED,

    /**
     * Система не разрешила приложению продвинутые уведомления.
     *
     * Это отдельное разрешение, и выдаёт его человек в системных настройках -
     * приложение может только привести его туда.
     */
    NOT_ALLOWED,

    /**
     * Разрешение есть, но само уведомление под условия не подходит - значит
     * недосмотр наш, и чинить надо здесь.
     */
    NOT_PROMOTABLE,

    /**
     * Всё, что зависит от приложения, выполнено. Показать плашку или нет - дело
     * оболочки: рисуют её все по-своему, а некоторые не рисуют вовсе.
     */
    READY;

    companion object {

        fun of(context: Context): LiveNotificationStatus {
            if (Build.VERSION.SDK_INT < 36) return UNSUPPORTED
            if (!MmkvManager.decodeSettingsBool(AppConfig.PREF_LIVE_NOTIFICATION, true)) {
                return DISABLED
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as? SystemNotificationManager
            if (manager?.canPostPromotedNotifications() != true) return NOT_ALLOWED

            // Проверяем настоящее уведомление, а не его подобие: собирает его тот же
            // код, что отдаёт уведомление системе
            val notification = runCatching {
                NotificationManager.buildNotification(context, null)
                    .setRequestPromotedOngoing(true)
                    .build()
            }.onFailure {
                LogUtil.e(AppConfig.TAG, "Live notification check failed", it)
            }.getOrNull() ?: return NOT_PROMOTABLE

            return if (NotificationCompat.hasPromotableCharacteristics(notification)) READY
            else NOT_PROMOTABLE
        }

        /** Экран системных настроек, где разрешают продвинутые уведомления. */
        fun settingsIntent(context: Context): Intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
}
