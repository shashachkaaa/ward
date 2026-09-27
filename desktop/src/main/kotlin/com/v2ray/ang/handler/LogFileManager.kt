package com.v2ray.ang.handler

import android.content.Context
import com.v2ray.ang.dto.V2rayConfig
import com.ward.desktop.DesktopPaths
import java.io.File

/**
 * Настольная версия: журналы ядра пишутся всегда, в logs/ рядом с журналом
 * приложения - посмотреть, почему не подключилось, иначе негде.
 */
object LogFileManager {
    const val ACCESS_LOG = "access.log"
    const val CORE_LOG = "core.log"

    fun applyFileLogging(log: V2rayConfig.LogBean, context: Context) {
        val paths = logFilePaths(context)
        log.access = paths?.first
        log.error = paths?.second
    }

    @Suppress("UNUSED_PARAMETER")
    fun logFilePaths(context: Context): Pair<String, String>? {
        val dir = DesktopPaths.logDir
        return File(dir, ACCESS_LOG).absolutePath to File(dir, CORE_LOG).absolutePath
    }
}
