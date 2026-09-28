package android.view

/** Нажатие клавиши - для onKeyDown скопированных экранов. */
class KeyEvent(val action: Int, val keyCode: Int) {
    companion object {
        const val ACTION_DOWN = 0
        const val KEYCODE_BACK = 4
        const val KEYCODE_MENU = 82
        const val KEYCODE_BUTTON_B = 97
    }
}
