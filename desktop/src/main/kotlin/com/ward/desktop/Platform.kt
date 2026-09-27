package com.ward.desktop

import java.awt.Desktop
import java.net.URI

object Platform {
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
