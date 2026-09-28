package com.ward.desktop

/** Управление окном из кода без Compose: спрятать в трей, показать. Подставляет Main. */
object WindowControl {
    @Volatile var onHide: () -> Unit = {}
    @Volatile var onShow: () -> Unit = {}
    fun hide() = onHide()
    fun show() = onShow()
}
