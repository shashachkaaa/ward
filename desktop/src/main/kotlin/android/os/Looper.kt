package android.os

import javax.swing.SwingUtilities

/** Главный поток - поток событий окна. */
class Looper private constructor(internal val main: Boolean) {
    companion object {
        private val mainLooper = Looper(true)
        private val other = Looper(false)
        @JvmStatic fun getMainLooper(): Looper = mainLooper
        @JvmStatic fun myLooper(): Looper? = if (SwingUtilities.isEventDispatchThread()) mainLooper else other
    }
}

open class Handler(private val looper: Looper = Looper.getMainLooper()) {
    fun post(r: Runnable): Boolean {
        SwingUtilities.invokeLater(r); return true
    }

    fun postDelayed(r: Runnable, delayMillis: Long): Boolean {
        javax.swing.Timer(delayMillis.toInt()) { r.run() }.apply { isRepeats = false; start() }
        return true
    }

    fun removeCallbacksAndMessages(token: Any?) {}
}

object SystemClock {
    @JvmStatic fun elapsedRealtime(): Long = System.nanoTime() / 1_000_000
    @JvmStatic fun uptimeMillis(): Long = System.nanoTime() / 1_000_000
}
