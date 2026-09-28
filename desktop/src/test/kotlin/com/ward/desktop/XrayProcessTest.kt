package com.ward.desktop

import android.content.Context
import com.v2ray.ang.core.CoreConfigManager
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Запуск настоящего ядра через CoreController - то, чем подключается приложение.
 * Нужен бинарник в resources/<система>: CI кладёт его туда перед сборкой, при
 * локальном запуске без него тест пропускается.
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
    fun coreStartsListensAndReportsStats() {
        if (!DesktopPaths.xrayBinary.exists()) {
            println("skip: no xray binary at ${DesktopPaths.xrayBinary}")
            return
        }
        AngConfigManager.importBatchConfig("trojan://pass@127.0.0.1:9?sni=example.com#Local", "", true)
        val guid = MmkvManager.getSelectServer()!!
        val config = CoreConfigManager.getV2rayConfig(Context.app, guid)
        assertTrue(config.status)

        val controller = CoreNativeManager.newCoreController(object : libv2ray.CoreCallbackHandler {
            override fun startup() = 0L
            override fun shutdown() = 0L
            override fun onEmitStatus(code: Long, message: String?) = 0L
        })
        controller.startLoop(config.content, 0)
        try {
            assertTrue(controller.isRunning)
            Socket().use { it.connect(InetSocketAddress("127.0.0.1", 10808), 1000) }
            Socket().use { it.connect(InetSocketAddress("127.0.0.1", 10809), 1000) }
            // API статистики поднят: ответ разбирается, пусть и пустой
            controller.queryAllOutboundTrafficStats()
        } finally {
            controller.stopLoop()
        }
        assertFalse(controller.isRunning)
    }

    @Test
    fun coreVersionIsReported() {
        if (!DesktopPaths.xrayBinary.exists()) return
        assertTrue(CoreNativeManager.getLibVersion().startsWith("Xray"), CoreNativeManager.getLibVersion())
    }
}
