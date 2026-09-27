package com.ward.desktop.core

import android.content.Context
import com.v2ray.ang.core.CoreConfigManager
import com.v2ray.ang.handler.SettingsManager
import com.ward.desktop.AppLog
import com.ward.desktop.DesktopPaths
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Ядро Xray дочерним процессом.
 *
 * Никакого JNI: libv2ray - обёртка gomobile под Android, а на компьютере рядом с
 * приложением лежит обычный бинарник xray. Конфиг собирается тем же кодом, что и
 * на Android, и пишется в файл - ядру его отдают путём.
 */
class XrayProcess {

    @Volatile
    private var process: Process? = null

    val isRunning: Boolean get() = process?.isAlive == true

    /**
     * Запускает ядро с выбранным сервером и ждёт, пока поднимется локальный порт.
     *
     * @throws IllegalStateException с понятным человеку текстом, если не вышло.
     */
    @Synchronized
    fun start(guid: String, onExit: (Int) -> Unit) {
        stop()
        killStale()

        val binary = executableBinary()

        // geoip.dat и geosite.dat из установки - в папку данных, откуда их читает ядро
        SettingsManager.initAssets(Context.app, DesktopPaths.bundledDir)

        val result = CoreConfigManager.getV2rayConfig(Context.app, guid)
        check(result.status) { "Не удалось собрать конфиг для этого сервера" }
        val configFile = File(DesktopPaths.dataDir, "config.json")
        configFile.writeText(result.content)

        val out = File(DesktopPaths.logDir, "xray-stdout.log")
        val pb = ProcessBuilder(binary.absolutePath, "run", "-c", configFile.absolutePath)
            .directory(DesktopPaths.dataDir)
            .redirectErrorStream(true)
            .redirectOutput(out)
        pb.environment()["XRAY_LOCATION_ASSET"] = DesktopPaths.assetsDir.absolutePath
        val p = pb.start()
        process = p
        pidFile.writeText(p.pid().toString())
        AppLog.write('I', TAG, "xray started, pid=${p.pid()}", null)

        // Ждём порт, а не просто время: на медленной машине ядро поднимается дольше
        val port = SettingsManager.getSocksPort()
        val deadline = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < deadline) {
            if (!p.isAlive) break
            if (portOpen(port)) break
            Thread.sleep(100)
        }
        if (!p.isAlive) {
            process = null
            val tail = runCatching { out.readLines().takeLast(5).joinToString("\n") }.getOrDefault("")
            throw IllegalStateException("Ядро не запустилось.\n$tail".trim())
        }

        p.onExit().thenAccept { exited ->
            if (process === exited) {
                process = null
                AppLog.write('W', TAG, "xray exited with ${exited.exitValue()}", null)
                onExit(exited.exitValue())
            }
        }
    }

    /**
     * Бинарник ядра, который можно запустить.
     *
     * Упаковщик под Linux теряет у файлов бит исполнения, а поставить его на
     * месте нельзя: /opt принадлежит root. Тогда ядро копируется в папку данных
     * и запускается оттуда - копия обновляется, когда меняется установленный файл.
     */
    private fun executableBinary(): File {
        val bundled = DesktopPaths.xrayBinary
        check(bundled.exists()) { "Не найден файл ядра: ${bundled.absolutePath}" }
        if (DesktopPaths.isWindows || bundled.canExecute()) return bundled
        if (bundled.setExecutable(true)) return bundled

        val copy = File(DesktopPaths.dataDir, "bin/xray")
        if (!copy.exists() || copy.length() != bundled.length() || copy.lastModified() < bundled.lastModified()) {
            copy.parentFile.mkdirs()
            bundled.copyTo(copy, overwrite = true)
        }
        copy.setExecutable(true)
        return copy
    }

    @Synchronized
    fun stop() {
        val p = process ?: return
        process = null
        p.destroy()
        if (!p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) p.destroyForcibly()
        pidFile.delete()
        AppLog.write('I', TAG, "xray stopped", null)
    }

    /**
     * Добивает ядро, оставшееся от прошлого запуска: если приложение убили, его
     * дочерний процесс живёт дальше и держит порт - новый на нём не поднимется.
     */
    private fun killStale() {
        val pid = runCatching { pidFile.readText().trim().toLong() }.getOrNull() ?: return
        ProcessHandle.of(pid).ifPresent { h ->
            val cmd = h.info().command().orElse("")
            if (cmd.contains("xray", ignoreCase = true)) {
                AppLog.write('W', TAG, "killing stale xray pid=$pid", null)
                h.destroyForcibly()
            }
        }
        pidFile.delete()
    }

    private fun portOpen(port: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 200); true }
    } catch (_: Exception) {
        false
    }

    private val pidFile get() = File(DesktopPaths.dataDir, "xray.pid")

    private companion object {
        const val TAG = "XrayProcess"
    }
}
