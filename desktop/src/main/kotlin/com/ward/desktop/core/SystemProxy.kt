package com.ward.desktop.core

import com.tencent.mmkv.MMKV
import com.ward.desktop.AppLog
import com.ward.desktop.DesktopPaths
import java.io.File

/**
 * Системный прокси: включить на время подключения и вернуть как было.
 *
 * Прежние значения сохраняются до изменения, а флаг «прокси выставлен нами»
 * лежит на диске: если приложение упало подключённым, при следующем запуске
 * настройки возвращаются - иначе у человека молча пропадает интернет.
 */
object SystemProxy {

    private const val TAG = "SystemProxy"
    private val store by lazy { MMKV.mmkvWithID("SYSTEM_PROXY") }

    /** Итог попытки: что получилось выставить, человеку это показывается. */
    data class Result(val ok: Boolean, val message: String)

    @Synchronized
    fun enable(httpPort: Int, socksPort: Int): Result = try {
        if (DesktopPaths.isWindows) Windows.enable(httpPort) else Linux.enable(httpPort, socksPort)
    } catch (e: Exception) {
        AppLog.write('E', TAG, "enable failed", e)
        Result(false, "Не удалось включить системный прокси: ${e.message}")
    }

    /**
     * Возвращает прежние настройки - но только если их меняли мы. Иначе выход
     * без подключения стёр бы прокси, который человек выставил себе сам.
     */
    @Synchronized
    fun disable() {
        if (!store.decodeBool(KEY_DIRTY, false)) return
        try {
            if (DesktopPaths.isWindows) Windows.restore() else Linux.restore()
        } catch (e: Exception) {
            AppLog.write('E', TAG, "restore failed", e)
        }
    }

    /** Вызывается при запуске: вернуть настройки после аварийного выхода. */
    fun recoverIfDirty() {
        if (store.decodeBool(KEY_DIRTY, false)) {
            AppLog.write('W', TAG, "restoring system proxy left from previous run", null)
        }
        disable()
    }

    private const val KEY_DIRTY = "dirty"

    // Адреса, которые в обход прокси: локальные сети и сам компьютер
    private val bypass = listOf(
        "localhost", "127.*", "10.*", "172.16.*", "172.17.*", "172.18.*", "172.19.*",
        "172.2*", "172.30.*", "172.31.*", "192.168.*"
    )

    private fun run(vararg cmd: String): Pair<Int, String> {
        val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        return p.waitFor() to out
    }

    private fun which(name: String): Boolean =
        System.getenv("PATH").orEmpty().split(File.pathSeparator).any { File(it, name).canExecute() }

    private object Windows {
        private const val KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings"

        fun enable(httpPort: Int): Result {
            if (!store.decodeBool(KEY_DIRTY, false)) {
                store.encode("win.enable", query("ProxyEnable") ?: "0x0")
                store.encode("win.server", query("ProxyServer") ?: "")
                store.encode("win.override", query("ProxyOverride") ?: "")
            }
            store.encode(KEY_DIRTY, true)
            reg("ProxyEnable", "REG_DWORD", "1")
            reg("ProxyServer", "REG_SZ", "127.0.0.1:$httpPort")
            reg("ProxyOverride", "REG_SZ", (bypass + "<local>").joinToString(";"))
            notifyChanged()
            return Result(true, "Системный прокси включён")
        }

        fun restore() {
            val enable = store.decodeString("win.enable") ?: "0x0"
            reg("ProxyEnable", "REG_DWORD", if (enable.endsWith("1")) "1" else "0")
            restoreString("ProxyServer", store.decodeString("win.server"))
            restoreString("ProxyOverride", store.decodeString("win.override"))
            notifyChanged()
            store.encode(KEY_DIRTY, false)
        }

        private fun restoreString(name: String, value: String?) {
            if (value.isNullOrEmpty()) run("reg", "delete", KEY, "/v", name, "/f") else reg(name, "REG_SZ", value)
        }

        private fun query(name: String): String? {
            val (code, out) = run("reg", "query", KEY, "/v", name)
            if (code != 0) return null
            return Regex("$name\\s+REG_\\w+\\s+(.*)").find(out)?.groupValues?.get(1)?.trim()
        }

        private fun reg(name: String, type: String, value: String) {
            val (code, out) = run("reg", "add", KEY, "/v", name, "/t", type, "/d", value, "/f")
            check(code == 0) { out.trim() }
        }

        /**
         * Сообщает системе, что настройки прокси поменялись. Без этого браузеры
         * и прочие программы увидят новый прокси только после перезапуска.
         */
        private fun notifyChanged() {
            val script = """
                ${'$'}sig = '[DllImport("wininet.dll")] public static extern bool InternetSetOption(IntPtr h, int o, IntPtr b, int l);'
                ${'$'}t = Add-Type -MemberDefinition ${'$'}sig -Name WinInet -Namespace Ward -PassThru
                ${'$'}t::InternetSetOption([IntPtr]::Zero, 39, [IntPtr]::Zero, 0) | Out-Null
                ${'$'}t::InternetSetOption([IntPtr]::Zero, 37, [IntPtr]::Zero, 0) | Out-Null
            """.trimIndent()
            runCatching { run("powershell", "-NoProfile", "-NonInteractive", "-Command", script) }
        }
    }

    private object Linux {
        private val desktop get() = System.getenv("XDG_CURRENT_DESKTOP").orEmpty().uppercase()

        fun enable(httpPort: Int, socksPort: Int): Result {
            val first = !store.decodeBool(KEY_DIRTY, false)
            val done = mutableListOf<String>()

            if (which("gsettings")) {
                if (first) store.encode("gnome.mode", gget("org.gnome.system.proxy", "mode") ?: "'none'")
                gset("org.gnome.system.proxy", "mode", "'manual'")
                for (scheme in listOf("http", "https")) {
                    gset("org.gnome.system.proxy.$scheme", "host", "'127.0.0.1'")
                    gset("org.gnome.system.proxy.$scheme", "port", httpPort.toString())
                }
                gset("org.gnome.system.proxy.socks", "host", "'127.0.0.1'")
                gset("org.gnome.system.proxy.socks", "port", socksPort.toString())
                gset("org.gnome.system.proxy", "ignore-hosts", bypass.plus("::1").joinToString(prefix = "[", postfix = "]") { "'$it'" })
                done += "GNOME"
            }

            val kwrite = listOf("kwriteconfig6", "kwriteconfig5").firstOrNull(::which)
            if (kwrite != null && desktop.contains("KDE")) {
                val kread = kwrite.replace("write", "read")
                if (first) store.encode("kde.type", run(kread, "--file", "kioslaverc", "--group", "Proxy Settings", "--key", "ProxyType").second.trim())
                kset(kwrite, "ProxyType", "1")
                kset(kwrite, "httpProxy", "http://127.0.0.1 $httpPort")
                kset(kwrite, "httpsProxy", "http://127.0.0.1 $httpPort")
                kset(kwrite, "socksProxy", "socks://127.0.0.1 $socksPort")
                kset(kwrite, "NoProxyFor", bypass.joinToString(","))
                kdeNotify()
                done += "KDE"
            }

            if (done.isEmpty()) {
                return Result(
                    false,
                    "Не нашлось, где включить системный прокси. Укажите в программах вручную: " +
                        "HTTP 127.0.0.1:$httpPort или SOCKS5 127.0.0.1:$socksPort"
                )
            }
            store.encode(KEY_DIRTY, true)
            return Result(true, "Системный прокси включён (${done.joinToString()})")
        }

        fun restore() {
            if (which("gsettings")) {
                gset("org.gnome.system.proxy", "mode", store.decodeString("gnome.mode") ?: "'none'")
            }
            val kwrite = listOf("kwriteconfig6", "kwriteconfig5").firstOrNull(::which)
            if (kwrite != null && store.decodeString("kde.type") != null) {
                kset(kwrite, "ProxyType", store.decodeString("kde.type")!!.ifBlank { "0" })
                kdeNotify()
            }
            store.encode(KEY_DIRTY, false)
        }

        private fun gget(schema: String, key: String): String? =
            run("gsettings", "get", schema, key).takeIf { it.first == 0 }?.second?.trim()

        private fun gset(schema: String, key: String, value: String) {
            run("gsettings", "set", schema, key, value)
        }

        private fun kset(tool: String, key: String, value: String) {
            run(tool, "--file", "kioslaverc", "--group", "Proxy Settings", "--key", key, value)
        }

        private fun kdeNotify() {
            runCatching {
                run("dbus-send", "--type=signal", "/KIO/Scheduler", "org.kde.KIO.Scheduler.reparseSlaveConfiguration", "string:")
            }
        }
    }
}
