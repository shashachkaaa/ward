package com.ward.desktop

import java.awt.Desktop
import java.net.URI

object Platform {
    /** Показать файл в папке - так на компьютере «делятся» готовым файлом. */
    fun revealFile(file: java.io.File) {
        if (DesktopPaths.isWindows) {
            runCatching { ProcessBuilder("explorer", "/select,", file.absolutePath).start() }
        } else {
            openFolder(file.parentFile ?: file)
        }
    }

    fun openFolder(dir: java.io.File) {
        runCatching { Desktop.getDesktop().open(dir) }.onFailure {
            val cmd = if (DesktopPaths.isWindows) listOf("explorer", dir.absolutePath) else listOf("xdg-open", dir.absolutePath)
            runCatching { ProcessBuilder(cmd).start() }
        }
    }

    fun openUrl(url: String) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                return
            }
        } catch (_: Exception) {
        }
        // На части Linux-окружений AWT не умеет открывать ссылки
        val cmd = if (DesktopPaths.isWindows) listOf("rundll32", "url.dll,FileProtocolHandler", url) else listOf("xdg-open", url)
        runCatching { ProcessBuilder(cmd).start() }
    }
}
