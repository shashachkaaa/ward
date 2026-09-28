package android.graphics.drawable

import android.graphics.Bitmap
import android.graphics.Canvas

/** Картинка для рисования на холсте. На компьютере это всегда готовый растр. */
open class Drawable(internal val source: Bitmap?) {
    open val intrinsicWidth: Int get() = source?.width ?: -1
    open val intrinsicHeight: Int get() = source?.height ?: -1
    private var bounds = java.awt.Rectangle()

    fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        bounds = java.awt.Rectangle(left, top, right - left, bottom - top)
    }

    open fun draw(canvas: Canvas) {
        val src = source ?: return
        val w = if (bounds.width > 0) bounds.width else canvas.width
        val h = if (bounds.height > 0) bounds.height else canvas.height
        canvas.bitmap.image.createGraphics().apply {
            setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            drawImage(src.image, bounds.x, bounds.y, w, h, null)
            dispose()
        }
    }
}

class BitmapDrawable(val bitmap: Bitmap) : Drawable(bitmap)
