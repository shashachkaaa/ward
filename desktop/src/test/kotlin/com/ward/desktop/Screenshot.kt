package com.ward.desktop

import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.File
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/** Подписка и ключи, чтобы на снимке было что показать. */
private fun seed() {
    val sub = com.v2ray.ang.dto.entities.SubscriptionItem(
        remarks = "Vanguard VPN",
        url = "https://example.com/sub",
        lastUpdated = System.currentTimeMillis(),
        announce = "Правила в сети теперь диктуете вы.\nЕсли подписка не работает - обновите её",
        color = "#d32f2f",
        trafficDownload = 290L shl 30,
        trafficTotal = 0,
        trafficExpire = System.currentTimeMillis() / 1000 + 29 * 86400
    )
    com.v2ray.ang.handler.MmkvManager.encodeSubscription("sub1", sub)
    com.v2ray.ang.handler.AngConfigManager.importBatchConfig(
        listOf(
            "vless://a3482e88-686a-4a58-8126-99c9df64b7bf@de.example.com:443?security=reality&sni=a.com&pbk=Z8xVbS5aYk2eTz7VmPq3rTn9sWx1yU4iOp6aSd8fGh0&type=tcp#🇩🇪 Германия",
            "vless://a3482e88-686a-4a58-8126-99c9df64b7bf@nl.example.com:443?security=tls&sni=b.com&type=ws#🇳🇱 Нидерланды",
            "trojan://p@fi.example.com:443?sni=c.com#🇫🇮 Финляндия"
        ).joinToString("\n"), "sub1", false
    )
    com.v2ray.ang.handler.AngConfigManager.importBatchConfig(
        "ss://" + java.util.Base64.getEncoder().encodeToString("aes-256-gcm:pw".toByteArray()) + "@1.2.3.4:8388#Домашний",
        com.v2ray.ang.AppConfig.STANDALONE_SUBSCRIPTION_ID, true
    )
    val guids = com.v2ray.ang.handler.MmkvManager.decodeServerList("sub1")
    guids.zip(listOf(84L, 212L, -1L)).forEach { (g, d) -> com.v2ray.ang.handler.MmkvManager.encodeServerTestDelayMillis(g, d) }
    com.v2ray.ang.handler.MmkvManager.setSelectServer(guids.first())
}

/**
 * Снимок окна для проверки внешнего вида без живого экрана: запускается под Xvfb
 * задачей screenshot, ждёт, пока окно отрисуется, и сохраняет экран в файл.
 */
fun main(args: Array<String>) {
    val out = File(args.getOrElse(0) { "build/screenshot.png" })
    val wait = args.getOrElse(1) { "6000" }.toLong()
    thread(isDaemon = true) {
        Thread.sleep(wait)
        val screen = Rectangle(Toolkit.getDefaultToolkit().screenSize)
        ImageIO.write(Robot().createScreenCapture(screen), "png", out)
        println("saved ${out.absolutePath}")
        exitProcess(0)
    }
    if (args.getOrNull(2) == "seed") seed()
    com.ward.desktop.main()
}
