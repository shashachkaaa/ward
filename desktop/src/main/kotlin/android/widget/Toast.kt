package android.widget

import android.content.Context
import com.ward.desktop.DesktopUi

/** Короткое сообщение. На компьютере показывается плашкой приложения. */
class Toast private constructor(private val text: String) {
    fun show() = DesktopUi.toast(text)
    fun cancel() {}

    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1

        @JvmStatic fun makeText(context: Context?, text: CharSequence, duration: Int) = Toast(text.toString())
        @JvmStatic fun makeText(context: Context?, resId: Int, duration: Int) =
            Toast(com.ward.desktop.AndroidResources.string(resId))
    }
}
