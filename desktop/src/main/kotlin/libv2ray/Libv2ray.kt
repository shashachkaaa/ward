package libv2ray

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ward.desktop.AppLog
import com.ward.desktop.DesktopPaths
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/*
 * Пакет libv2ray для компьютера.
 *
 * На Android это обёртка gomobile над Xray внутри процесса приложения. Здесь то
 * же самое сделано снаружи: ядро - обычный бинарник xray, запущенный дочерним
 * процессом, а замеры и статистика идут через его локальные входы и API. Имена и
 * сигнатуры повторяют Android, поэтому код поверх (CoreNativeManager, PingManager)
 * скопирован без правок.
 */

interface CoreCallbackHandler {
    fun startup(): Long
    fun shutdown(): Long
    fun onEmitStatus(code: Long, message: String?): Long
}

interface ProcessFinder {
    fun findProcessByConnection(network: String, srcIP: String, srcPort: Long, destIP: String, destPort: Long): Long
}

object Libv2ray {

    @Volatile internal var assetPath: String = DesktopPaths.assetsDir.absolutePath

    @JvmStatic
    fun initCoreEnv(assetPath: String, deviceId: String) {
        this.assetPath = assetPath
    }

    @JvmStatic
    fun newCoreController(handler: CoreCallbackHandler): CoreController = CoreController(handler)

    @JvmStatic
    fun reconcileBrowserDialer(addr: String) {}

    /** Версия ядра - как её печатает сам бинарник. */
    @JvmStatic
    fun checkVersionX(): String {
        val p = ProcessBuilder(executableXray().absolutePath, "version").redirectErrorStream(true).start()
        val first = p.inputStream.bufferedReader().readLine().orEmpty()
        p.waitFor(5, TimeUnit.SECONDS)
        // «Xray 26.3.27 (Xray, Penetrates Everything.) ...» -> «Xray 26.3.27»
        return first.split(' ').take(2).joinToString(" ").ifBlank { "Unknown" }
    }

    /**
     * Замер через один сервер: временное ядро с этим конфигом и локальным SOCKS,
     * через который уходит запрос. Как у libv2ray - метод GET и время ответа.
     */
    @JvmStatic
    fun measureOutboundDelay(config: String, url: String): Long {
        val port = ServerSocket(0).use { it.localPort }
        val json = JsonParser.parseString(config).asJsonObject
        json.add("inbounds", JsonArray().apply {
            add(JsonObject().apply {
                addProperty("tag", "measure")
                addProperty("listen", "127.0.0.1")
                addProperty("port", port)
                addProperty("protocol", "socks")
                add("settings", JsonObject().apply { addProperty("auth", "noauth") })
            })
        })
        val controller = CoreController(null)
        controller.startLoop(json.toString(), 0)
        try {
            return httpDelay(url, Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port)), 10_000)
        } finally {
            controller.stopLoop()
        }
    }

    /** Отпечаток сертификата сервера TLS: SHA-256 от DER первого сертификата цепочки. */
    @JvmStatic
    fun fetchTlsCertSha256(requestJson: String): String {
        val req = JsonParser.parseString(requestJson).asJsonObject
        val address = req["address"].asString
        val port = req["port"].asInt
        val sni = req["serverName"]?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }
        val timeout = req["timeoutMs"]?.asInt ?: 5000
        val result = JsonObject()
        try {
            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(c: Array<X509Certificate>, a: String) {}
                override fun checkServerTrusted(c: Array<X509Certificate>, a: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            })
            val ctx = SSLContext.getInstance("TLS").apply { init(null, trustAll, null) }
            val raw = Socket().apply { connect(InetSocketAddress(address, port), timeout); soTimeout = timeout }
            (ctx.socketFactory.createSocket(raw, sni ?: address, port, true) as SSLSocket).use { s ->
                if (sni != null) s.sslParameters = s.sslParameters.apply { serverNames = listOf(SNIHostName(sni)) }
                s.startHandshake()
                val cert = s.session.peerCertificates.first()
                val digest = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
                result.addProperty("sha256", digest.joinToString("") { "%02x".format(it) })
                result.addProperty("error", "")
            }
        } catch (e: Exception) {
            result.addProperty("sha256", "")
            result.addProperty("error", e.message ?: e.javaClass.simpleName)
        }
        return result.toString()
    }

    /** QUIC из JVM без своей реализации не открыть - честно отвечаем ошибкой. */
    @JvmStatic
    fun fetchQuicCertSha256(requestJson: String): String =
        JsonObject().apply {
            addProperty("sha256", "")
            addProperty("error", "QUIC fingerprint is not supported on desktop")
        }.toString()

    internal fun httpDelay(url: String, proxy: Proxy, timeoutMs: Int): Long {
        // Первый запрос прогревает соединение до сервера, как делает libv2ray;
        // меряем второй - иначе в задержку попадает рукопожатие с самим сервером
        repeat(2) { attempt ->
            val start = System.nanoTime()
            val conn = URL(url).openConnection(proxy) as java.net.HttpURLConnection
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.instanceFollowRedirects = false
            val code = conn.responseCode
            conn.inputStream.use { it.readBytes() }
            if (code >= 400) throw IllegalStateException("$url answered with $code")
            if (attempt == 1) return (System.nanoTime() - start) / 1_000_000
        }
        return -1
    }

    /**
     * Бинарник ядра, который можно запустить. Упаковщик под Linux теряет у файлов
     * бит исполнения, а /opt принадлежит root - тогда ядро копируется в папку данных.
     */
    @JvmStatic
    fun executableXray(): File {
        val bundled = DesktopPaths.xrayBinary
        check(bundled.exists()) { "Xray core not found: ${bundled.absolutePath}" }
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
}

/**
 * Одно запущенное ядро. startLoop поднимает процесс xray с конфигом и ждёт, пока
 * откроются его локальные входы; stopLoop его останавливает.
 */
class CoreController internal constructor(private val handler: CoreCallbackHandler?) {

    @Volatile private var process: Process? = null
    @Volatile private var config: JsonObject? = null
    @Volatile private var metricsPort: Int = 0
    private var configFile: File? = null

    val isRunning: Boolean get() = process?.isAlive == true

    fun registerProcessFinder(finder: ProcessFinder) {}

    /**
     * @param tunFd Дескриптор туннеля - на компьютере его нет, всегда 0.
     */
    @Synchronized
    fun startLoop(configContent: String, tunFd: Int) {
        stopLoop()
        val json = JsonParser.parseString(configContent).asJsonObject
        withMetrics(json)
        quietAccessLog(json)
        config = json

        val file = File.createTempFile("xray-", ".json", File(DesktopPaths.dataDir, "run").apply { mkdirs() })
        file.writeText(json.toString())
        configFile = file

        val log = File(DesktopPaths.logDir, "xray-stdout.log")
        if (log.length() > 5_000_000) log.delete()
        // Журнал общий для всех запусков; ошибку этого запуска ищем после этой отметки
        val logStart = log.length()
        val pb = ProcessBuilder(Libv2ray.executableXray().absolutePath, "run", "-c", file.absolutePath)
            .directory(DesktopPaths.dataDir)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(log))
        pb.environment()["XRAY_LOCATION_ASSET"] = Libv2ray.assetPath
        val p = pb.start()
        process = p
        CoreProcesses.register(p)

        val ports = inboundPorts(json)
        val deadline = System.currentTimeMillis() + 8000
        while (System.currentTimeMillis() < deadline && p.isAlive) {
            if (ports.isEmpty() || ports.all(::portOpen)) break
            Thread.sleep(50)
        }
        if (!p.isAlive) {
            process = null
            file.delete()
            val fresh = runCatching {
                java.io.RandomAccessFile(log, "r").use { raf -> raf.seek(logStart); ByteArray((raf.length() - logStart).toInt()).also(raf::readFully) }
                    .decodeToString().lines()
            }.getOrDefault(emptyList())
            // Самое полезное - строка «Failed to start: ...», остальное - шапка ядра
            val reason = fresh.lastOrNull { it.contains("Failed to start") }
                ?.substringAfter("Failed to start: ")
                ?: fresh.lastOrNull { it.isNotBlank() }
            throw IllegalStateException(reason ?: "Xray exited with ${p.exitValue()}")
        }
        handler?.startup()
        p.onExit().thenAccept { exited ->
            if (process === exited) {
                process = null
                AppLog.write('W', "CoreController", "xray exited with ${exited.exitValue()}", null)
                handler?.shutdown()
            }
        }
    }

    @Synchronized
    fun stopLoop() {
        val p = process ?: return
        process = null
        p.destroy()
        if (!p.waitFor(3, TimeUnit.SECONDS)) p.destroyForcibly()
        CoreProcesses.unregister(p)
        configFile?.delete()
        configFile = null
    }

    /** Задержка через само подключение - запрос через его SOCKS-вход. */
    fun measureDelay(url: String): Long {
        val cfg = config ?: throw IllegalStateException("core is not running")
        val socks = cfg.getAsJsonArray("inbounds")?.map { it.asJsonObject }
            ?.firstOrNull { it["protocol"]?.asString == "socks" }
            ?: throw IllegalStateException("no local socks inbound")
        val port = socks["port"].asInt
        return Libv2ray.httpDelay(url, Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port)), 10_000)
    }

    /** Накопленные счётчики с прошлого опроса: страница метрик отдаёт итог, а не приращение. */
    private val lastCounters = HashMap<String, Long>()

    /**
     * Счётчики трафика по исходящим, со сбросом - в том же виде, что у libv2ray:
     * «tag,direction,value;...».
     *
     * Читаются со страницы метрик ядра (/debug/vars) обычным HTTP-запросом.
     * Раньше здесь раз в секунду запускался xray api statsquery - на Windows
     * каждый запуск 36-мегабайтного exe стоит заметного процессора, а Защитник
     * ещё и проверяет его всякий раз, и это отнимало скорость у самого ядра.
     */
    @Synchronized
    fun queryAllOutboundTrafficStats(): String {
        if (!isRunning || metricsPort == 0) return ""
        val body = runCatching {
            val conn = URL("http://127.0.0.1:$metricsPort/debug/vars").openConnection(Proxy.NO_PROXY) as java.net.HttpURLConnection
            conn.connectTimeout = 500
            conn.readTimeout = 500
            conn.inputStream.use { it.readBytes().decodeToString() }
        }.getOrNull() ?: return ""
        val outbound = runCatching {
            JsonParser.parseString(body).asJsonObject.getAsJsonObject("stats").getAsJsonObject("outbound")
        }.getOrNull() ?: return ""
        val parts = mutableListOf<String>()
        for ((tag, value) in outbound.entrySet()) {
            val o = value.asJsonObject
            for (direction in listOf("uplink", "downlink")) {
                val total = o[direction]?.asLong ?: continue
                val key = "$tag>$direction"
                val delta = (total - (lastCounters[key] ?: 0L)).coerceAtLeast(0L)
                lastCounters[key] = total
                parts += "$tag,$direction,$delta"
            }
        }
        return parts.joinToString(";")
    }

    /**
     * Страница метрик ядра - только если в конфиге включена статистика, как у
     * основного подключения. Слушает сама, без входа и правила маршрутизации.
     */
    private fun withMetrics(json: JsonObject) {
        metricsPort = 0
        lastCounters.clear()
        if (json["stats"] == null) return
        val port = ServerSocket(0).use { it.localPort }
        json.add("metrics", JsonObject().apply {
            addProperty("tag", METRICS_TAG)
            addProperty("listen", "127.0.0.1:$port")
        })
        metricsPort = port
    }

    /**
     * Журнал доступа пишет строку на каждое соединение. Оставляем его, только
     * если журнал просили подробный; иначе он забивает файл и экран журнала.
     */
    private fun quietAccessLog(json: JsonObject) {
        val log = json.getAsJsonObject("log") ?: JsonObject().also { json.add("log", it) }
        val level = log["loglevel"]?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
        val access = log["access"]?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
        if (access.isEmpty() && level != "debug" && level != "info") log.addProperty("access", "none")
    }

    private fun inboundPorts(json: JsonObject): List<Int> =
        json.getAsJsonArray("inbounds")?.mapNotNull { it.asJsonObject["port"]?.takeIf { p -> p.isJsonPrimitive }?.asInt }.orEmpty()

    private fun portOpen(port: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 100); true }
    } catch (_: Exception) {
        false
    }

    private companion object {
        const val METRICS_TAG = "ward-metrics"
    }
}

/**
 * Все запущенные процессы ядра. При выходе их надо остановить, а после
 * аварийного выхода - добить оставшиеся с прошлого раза: они держат порты.
 */
object CoreProcesses {
    private val live = java.util.Collections.synchronizedSet(HashSet<Process>())
    private val pidFile get() = File(DesktopPaths.dataDir, "xray.pids")

    fun register(p: Process) { live += p; save() }
    fun unregister(p: Process) { live -= p; save() }

    fun stopAll() {
        live.toList().forEach { it.destroy(); if (!it.waitFor(2, TimeUnit.SECONDS)) it.destroyForcibly() }
        live.clear(); save()
    }

    fun killStale() {
        runCatching { pidFile.readLines() }.getOrDefault(emptyList()).mapNotNull { it.trim().toLongOrNull() }.forEach { pid ->
            ProcessHandle.of(pid).ifPresent { h ->
                if (h.info().command().orElse("").contains("xray", ignoreCase = true)) h.destroyForcibly()
            }
        }
        pidFile.delete()
    }

    private fun save() = runCatching { pidFile.writeText(live.joinToString("\n") { it.pid().toString() }) }
}
