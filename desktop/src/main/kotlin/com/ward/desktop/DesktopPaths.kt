package com.ward.desktop

import java.io.File

/**
 * Где приложение держит свои файлы.
 *
 * Windows - %APPDATA%\Ward, Linux - $XDG_DATA_HOME/ward или ~/.local/share/ward.
 * Бинарник xray и geo-файлы лежат рядом с установленным приложением: их туда
 * кладёт установщик, и их обновление - это обновление самого приложения.
 */
object DesktopPaths {
    val isWindows: Boolean = System.getProperty("os.name").lowercase().contains("win")

    val dataDir: File by lazy {
        // Свойство ward.dataDir - для тестов и переносной установки
        val base = System.getProperty("ward.dataDir")?.let(::File) ?: if (isWindows) {
            File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Ward")
        } else {
            val xdg = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
            File(xdg ?: (System.getProperty("user.home") + "/.local/share"), "ward")
        }
        base.apply { mkdirs() }
    }

    val logDir: File by lazy { File(dataDir, "logs").apply { mkdirs() } }

    /** Папка, откуда xray берёт geoip.dat и geosite.dat. */
    val assetsDir: File by lazy { File(dataDir, "assets").apply { mkdirs() } }

    /**
     * Ресурсы, приложенные к сборке. compose.application.resources.dir задаёт
     * упаковщик; при запуске из исходников смотрим в resources/<система>.
     */
    val bundledDir: File by lazy {
        System.getProperty("compose.application.resources.dir")?.let { File(it) }
            ?: File("resources/" + if (isWindows) "windows" else "linux")
    }

    val xrayBinary: File get() = File(bundledDir, if (isWindows) "xray.exe" else "xray")
}
