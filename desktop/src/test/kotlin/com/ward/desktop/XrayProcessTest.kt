package com.ward.desktop

import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.ward.desktop.core.XrayProcess
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Запуск настоящего ядра. Нужен бинарник в resources/<система>: CI кладёт его
 * туда перед сборкой, при локальном запуске без него тест пропускается.
 */
class XrayProcessTest {

    companion object {
        init {
            if (System.getProperty("ward.dataDir") == null) {
                System.setProperty("ward.dataDir", Files.createTempDirectory("ward-xray").toString())
            }
        }
    }

    @Test
    fun coreStartsAndListens() {
        if (!DesktopPaths.xrayBinary.exists()) {
            println("skip: no xray binary at ${DesktopPaths.xrayBinary}")
            return
        }
        AngConfigManager.importBatchConfig(
            "trojan://pass@127.0.0.1:9?sni=example.com#Local", "", true
        )
        val guid = MmkvManager.getSelectServer()!!
        val xray = XrayProcess()
        xray.start(guid) {}
        try {
            assertTrue(xray.isRunning)
            Socket().use { it.connect(InetSocketAddress("127.0.0.1", 10808), 1000) }
            Socket().use { it.connect(InetSocketAddress("127.0.0.1", 10809), 1000) }
        } finally {
            xray.stop()
        }
        assertFalse(xray.isRunning)
    }
}
