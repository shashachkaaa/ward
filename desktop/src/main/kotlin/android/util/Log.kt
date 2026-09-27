package android.util

import com.ward.desktop.AppLog

object Log {
    const val VERBOSE = 2
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6
    const val ASSERT = 7

    @JvmStatic fun println(priority: Int, tag: String?, msg: String): Int =
        AppLog.write("??VDIWEA".getOrElse(priority) { 'I' }, tag, msg, null)
    @JvmStatic fun v(tag: String?, msg: String, tr: Throwable?): Int = AppLog.write('V', tag, msg, tr)
    @JvmStatic fun v(tag: String?, msg: String): Int = AppLog.write('V', tag, msg, null)
    @JvmStatic fun d(tag: String?, msg: String): Int = AppLog.write('D', tag, msg, null)
    @JvmStatic fun i(tag: String?, msg: String): Int = AppLog.write('I', tag, msg, null)
    @JvmStatic fun w(tag: String?, msg: String): Int = AppLog.write('W', tag, msg, null)
    @JvmStatic fun w(tag: String?, msg: String, tr: Throwable?): Int = AppLog.write('W', tag, msg, tr)
    @JvmStatic fun e(tag: String?, msg: String): Int = AppLog.write('E', tag, msg, null)
    @JvmStatic fun e(tag: String?, msg: String, tr: Throwable?): Int = AppLog.write('E', tag, msg, tr)
    @JvmStatic fun d(tag: String?, msg: String, tr: Throwable?): Int = AppLog.write('D', tag, msg, tr)
    @JvmStatic fun i(tag: String?, msg: String, tr: Throwable?): Int = AppLog.write('I', tag, msg, tr)
}
