package com.ward.desktop

/**
 * Системные уведомления. Показывает их трей: Main подставляет сюда свою функцию,
 * а без трея уведомление уходит плашкой в окно.
 */
object DesktopNotifications {
    @Volatile var sender: ((title: String, text: String) -> Unit)? = null

    fun notify(title: String, text: String) {
        sender?.invoke(title, text) ?: DesktopUi.toast("$title: $text")
    }
}
