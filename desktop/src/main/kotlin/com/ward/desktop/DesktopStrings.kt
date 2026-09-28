package com.ward.desktop

import java.util.Locale

/** Немногие строки, которых нет у Android-версии: только про отличия компьютера. */
object DesktopStrings {
    private val ru = Locale.getDefault().language == "ru"
    val notOnDesktop: String get() = if (ru) "На компьютере это недоступно" else "Not available on desktop"
    val open: String get() = if (ru) "Открыть Ward" else "Open Ward"
    val connect: String get() = if (ru) "Подключить" else "Connect"
    val disconnect: String get() = if (ru) "Отключить" else "Disconnect"
    val exit: String get() = if (ru) "Выход" else "Exit"
    val connected: String get() = if (ru) "подключено" else "connected"
}
