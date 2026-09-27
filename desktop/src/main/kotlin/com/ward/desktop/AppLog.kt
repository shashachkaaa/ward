package com.ward.desktop

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Журнал приложения: в консоль и в файл logs/ward.log, который обрезается при запуске. */
object AppLog {
    private val file: File by lazy {
        File(DesktopPaths.logDir, "ward.log").apply {
            if (exists() && length() > 2_000_000) delete()
        }
    }
    private val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun write(level: Char, tag: String?, msg: String, tr: Throwable?): Int {
        val line = buildString {
            append(synchronized(time) { time.format(Date()) }).append(' ').append(level).append('/')
            append(tag ?: "Ward").append(": ").append(msg)
            if (tr != null) {
                val sw = StringWriter()
                tr.printStackTrace(PrintWriter(sw))
                append('\n').append(sw)
            }
        }
        System.err.println(line)
        try {
            synchronized(this) { file.appendText(line + "\n") }
        } catch (_: Exception) {
        }
        return 0
    }
}
