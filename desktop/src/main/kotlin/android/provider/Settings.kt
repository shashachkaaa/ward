package android.provider

/** Экраны системных настроек Android. На компьютере их нет, переход никуда не ведёт. */
object Settings {
    const val ACTION_VPN_SETTINGS = "android.settings.VPN_SETTINGS"
    const val ACTION_APPLICATION_DETAILS_SETTINGS = "android.settings.APPLICATION_DETAILS_SETTINGS"
    const val ACTION_APP_NOTIFICATION_SETTINGS = "android.settings.APP_NOTIFICATION_SETTINGS"
    const val ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS = "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"
    const val ACTION_MANAGE_UNKNOWN_APP_SOURCES = "android.settings.MANAGE_UNKNOWN_APP_SOURCES"
    const val EXTRA_APP_PACKAGE = "android.provider.extra.APP_PACKAGE"

    object Secure {
        const val ANDROID_ID = "android_id"
    }
}

object OpenableColumns {
    const val DISPLAY_NAME = "_display_name"
    const val SIZE = "_size"
}
