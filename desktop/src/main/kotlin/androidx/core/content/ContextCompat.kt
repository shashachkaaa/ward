package androidx.core.content

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.Bitmap

object ContextCompat {
    /** Значок из ресурсов - растр из mipmap. */
    @JvmStatic
    fun getDrawable(context: Context, id: Int): Drawable? = runCatching {
        val name = com.ward.desktop.AndroidResources.mipmapName(id)
        val stream = com.ward.desktop.AndroidResources::class.java.getResourceAsStream("/android-res/mipmap/$name.png")!!
        BitmapDrawable(android.graphics.BitmapFactory.decodeStream(stream)!!)
    }.getOrNull()

    const val RECEIVER_EXPORTED = 2
    const val RECEIVER_NOT_EXPORTED = 4

    @JvmStatic
    fun registerReceiver(context: Context, receiver: android.content.BroadcastReceiver, filter: android.content.IntentFilter, flags: Int): android.content.Intent? =
        context.registerReceiver(receiver, filter)

    @JvmStatic fun getColor(context: Context, id: Int): Int = 0xff000000.toInt()
}
