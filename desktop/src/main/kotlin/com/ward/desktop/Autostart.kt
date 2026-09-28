package com.ward.desktop

import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.LogUtil
import java.io.File

/**
 * Запуск вместе с системой - настройка «Автозапуск» с Android, только здесь это
 * не приёмник загрузки, а запись в автозагрузку: ключ Run в реестре на Windows,
 * файл в ~/.config/autostart на Linux. Запущенный так Ward сразу прячется в трей.
 */
object Autostart {

    const val HIDDEN_ARG = "--hidden"

    /** Привести автозагрузку в соответствие с настройкой. */
    fun sync() {
        val enabled = MmkvManager.decodeStartOnBoot()
        runCatching { if (DesktopPaths.isWindows) windows(enabled) else linux(enabled) }
            .onFailure { LogUtil.e("Autostart", "Failed to update autostart", it) }
    }

    /** Команда запуска установленного приложения; при запуске из исходников её нет. */
    private fun launcher(): String? =
        ProcessHandle.current().info().command().orElse(null)?.takeIf { !it.endsWith("java") && !it.endsWith("java.exe") }

    private fun windows(enabled: Boolean) {
        val key = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        if (enabled) {
            val exe = launcher() ?: return
            ProcessBuilder("reg", "add", key, "/v", "Ward", "/t", "REG_SZ", "/d", "\"$exe\" $HIDDEN_ARG", "/f").start().waitFor()
        } else {
            ProcessBuilder("reg", "delete", key, "/v", "Ward", "/f").start().waitFor()
        }
    }

    private fun linux(enabled: Boolean) {
        val base = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() } ?: (System.getProperty("user.home") + "/.config")
        val file = File(base, "autostart/ward.desktop")
        if (!enabled) {
            file.delete(); return
        }
        val exe = launcher() ?: return
        file.parentFile.mkdirs()
        file.writeText(
            """
            [Desktop Entry]
            Type=Application
            Name=Ward
            Exec="$exe" $HIDDEN_ARG
            X-GNOME-Autostart-enabled=true
            """.trimIndent() + "\n"
        )
    }
}
