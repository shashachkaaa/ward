package com.v2ray.ang.handler

import android.annotation.SuppressLint
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
 * Гадать по её отсутствию бесполезно, поэтому каждое условие спрашивается отдельно
 * и показывается в настройках. Журнал для этого не годится: на части прошивок его
 * из приложения не прочитать.
 *
 * Отвечают оба вопроса, а не первый попавшийся. Останавливайся проверка на
 * неразрешённом разрешении - мы бы так и не узнали, готова ли наша сторона, а это
 * разные починки: одна в системных настройках, другая в коде.
 *
 * @param allowed Система разрешила приложению продвинутые уведомления. Отдельное
 *   разрешение, выдаёт его человек; приложение может только привести его туда.
 * @param promotable Само уведомление проходит по условиям системы. Если нет -
 *   недосмотр наш, и чинить надо здесь.
 */
data class LiveNotificationStatus(
    val allowed: Boolean,
    val promotable: Boolean
) {

    companion object {

        /** null - живых уведомлений на этой системе не бывает или они выключены. */
        fun of(context: Context): LiveNotificationStatus? {
            if (Build.VERSION.SDK_INT < 36) return null
            if (!MmkvManager.decodeSettingsBool(AppConfig.PREF_LIVE_NOTIFICATION, true)) {
                return null
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as? SystemNotificationManager
            val allowed = manager?.canPostPromotedNotifications() == true

            // Проверяем настоящее уведомление, а не его подобие: собирает его тот же
            // код, что отдаёт уведомление системе
            val notification = runCatching {
                NotificationManager.buildNotification(context, null)
                    .setRequestPromotedOngoing(true)
                    .build()
            }.onFailure {
                LogUtil.e(AppConfig.TAG, "Live notification check failed", it)
            }.getOrNull()

            val promotable = notification != null &&
                    NotificationCompat.hasPromotableCharacteristics(notification)

            return LiveNotificationStatus(allowed = allowed, promotable = promotable)
        }

        /**
         * Экран системных настроек, где разрешают продвинутые уведомления.
         *
         * Есть он не везде: часть оболочек этого экрана не завела, и переход
         * приводит на обычную страницу уведомлений приложения.
         *
         * Действие из Android 16, и проверка кода видит его значение вписанным в наш
         * код. Так и есть, но попасть сюда раньше нельзя: [of] ниже шестнадцатой
         * версии отвечает null, и звать этот экран становится некому.
         */
        @SuppressLint("InlinedApi")
        fun settingsIntent(context: Context): Intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
}
