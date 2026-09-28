package com.ward.desktop

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.RandomAccessFile

/**
 * «logcat» для компьютера: процесс, который отдаёт журнал приложения и журнал
 * ядра - сперва то, что в них уже есть, потом новые строки по мере записи.
 *
 * Экран журнала на Android читает вывод процесса logcat построчно. Отдав ему
 * такой же Process, экран удаётся оставить копией.
 */
object DesktopLogcat {

    private val files: List<File>
        get() = listOf("ward.log", "xray-stdout.log", com.v2ray.ang.handler.LogFileManager.CORE_LOG).map { File(DesktopPaths.logDir, it) }

    fun start(): Process = TailProcess(files)

    /** Очистить журналы - как logcat -c. */
    fun clear() {
        files.forEach { f -> runCatching { if (f.exists()) RandomAccessFile(f, "rw").use { it.setLength(0) } } }
    }

    private class TailProcess(private val files: List<File>) : Process() {
        private val sink = PipedOutputStream()
        private val source = PipedInputStream(sink, 1 shl 16)
        @Volatile private var alive = true

        private val worker = Thread({
            val positions = LongArray(files.size)
            try {
                while (alive) {
                    var any = false
                    files.forEachIndexed { i, f ->
                        if (!f.exists()) return@forEachIndexed
                        val len = f.length()
                        if (len < positions[i]) positions[i] = 0 // файл очистили
                        if (len > positions[i]) {
                            RandomAccessFile(f, "r").use { raf ->
                                raf.seek(positions[i])
                                val buf = ByteArray((len - positions[i]).toInt().coerceAtMost(1 shl 20))
                                raf.readFully(buf)
                                positions[i] += buf.size
                                sink.write(buf)
                                any = true
                            }
                        }
                    }
                    if (any) sink.flush() else Thread.sleep(300)
                }
            } catch (_: Exception) {
            } finally {
                runCatching { sink.close() }
            }
        }, "desktop-logcat").apply { isDaemon = true; start() }

        override fun getInputStream(): InputStream = source
        override fun getErrorStream(): InputStream = InputStream.nullInputStream()
        override fun getOutputStream(): OutputStream = OutputStream.nullOutputStream()
        override fun waitFor(): Int { worker.join(); return 0 }
        override fun exitValue(): Int = if (alive) throw IllegalThreadStateException() else 0
        override fun destroy() { alive = false; worker.interrupt() }
        override fun isAlive(): Boolean = alive
    }
}
