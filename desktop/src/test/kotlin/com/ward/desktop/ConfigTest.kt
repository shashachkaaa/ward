package com.ward.desktop

import android.content.Context
import com.google.gson.JsonParser
import com.v2ray.ang.core.CoreConfigManager
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigTest {

    companion object {
        init {
            System.setProperty("ward.dataDir", Files.createTempDirectory("ward-test").toString())
        }
    }

    @BeforeTest
    fun init() {
        // Пути читаются лениво - свойство выше успевает раньше
        DesktopPaths.dataDir
    }

    @Test
    fun vlessLinkBuildsProxyConfig() {
        val link = "vless://a3482e88-686a-4a58-8126-99c9df64b7bf@example.com:443" +
            "?encryption=none&security=reality&sni=www.microsoft.com&fp=chrome" +
            "&pbk=Z8xVbS5aYk2eTz7VmPq3rTn9sWx1yU4iOp6aSd8fGh0&sid=6ba85179e30d4fc2" +
            "&type=tcp&flow=xtls-rprx-vision#Test%20server"
        val result = AngConfigManager.importBatchConfig(link, "", true)
        assertEquals(1, result.count, "ссылка должна разобраться в один сервер")

        val guid = MmkvManager.getSelectServer()!!
        assertEquals("Test server", MmkvManager.decodeServerConfig(guid)!!.remarks)

        val config = CoreConfigManager.getV2rayConfig(Context.app, guid)
        assertTrue(config.status, "конфиг должен собраться")
        val json = JsonParser.parseString(config.content).asJsonObject

        val inbounds = json.getAsJsonArray("inbounds").map { it.asJsonObject }
        assertEquals(listOf("socks", "http"), inbounds.map { it["protocol"].asString })
        assertEquals(listOf(10808, 10809), inbounds.map { it["port"].asInt })
        assertTrue(inbounds.all { it["listen"].asString == "127.0.0.1" }, "наружу слушать нельзя")

        val proxy = json.getAsJsonArray("outbounds").map { it.asJsonObject }.first { it["tag"].asString == "proxy" }
        assertEquals("vless", proxy["protocol"].asString)
        val stream = proxy.getAsJsonObject("streamSettings")
        assertEquals("reality", stream["security"].asString)
    }

    @Test
    fun subscriptionBodyInBase64IsParsed() {
        val links = listOf(
            "trojan://pass@t.example.com:443?sni=t.example.com#Trojan",
            "ss://" + java.util.Base64.getEncoder().encodeToString("aes-256-gcm:pw".toByteArray()) + "@1.2.3.4:8388#SS"
        ).joinToString("\n")
        val body = java.util.Base64.getEncoder().encodeToString(links.toByteArray())
        val result = AngConfigManager.importBatchConfig(body, "sub-test", false)
        assertEquals(2, result.count)
    }
}

class SystemProxyTest {
    @Test
    fun disableWithoutEnableTouchesNothing() {
        // Флаг не выставлен - disable() обязан вернуться, ничего не запуская:
        // на Windows он иначе стёр бы прокси пользователя из реестра
        com.ward.desktop.core.SystemProxy.disable()
        val store = com.tencent.mmkv.MMKV.mmkvWithID("SYSTEM_PROXY")
        kotlin.test.assertNull(store.allKeys()?.firstOrNull { it != "dirty" })
    }
}
