package com.v2ray.ang.handler

import android.util.Base64
import android.util.Log
import com.v2ray.ang.dto.entities.SubscriptionItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import java.util.Base64 as JavaBase64

/**
 * Разбор заголовков ответа подписки.
 *
 * Формат этих заголовков задаёт панель на той стороне, а не приложение. Меняется он
 * не спрашивая, и сломанный разбор виден плохо: подписка обновилась, конфиги приехали,
 * а лимит молча обнулился или в имени оказался мусор. Поэтому здесь проверяется не
 * только удачный случай, но и то, что приходит на деле: закодированное имя, лишние
 * пары в счётчиках, дробный интервал, отсутствующие заголовки.
 */
class SubscriptionHeadersTest {

    private lateinit var mockBase64: MockedStatic<Base64>
    private lateinit var mockLog: MockedStatic<Log>

    @Before
    fun setUp() {
        mockLog = mockStatic(Log::class.java, Mockito.RETURNS_DEFAULTS)
        mockBase64 = mockStatic(Base64::class.java)
        mockBase64.`when`<ByteArray> {
            Base64.decode(Mockito.anyString(), Mockito.anyInt())
        }.thenAnswer { invocation ->
            // Как на устройстве: испорченный ввод роняет исключение, а не молчит
            JavaBase64.getDecoder().decode(invocation.arguments[0] as String)
        }
    }

    @After
    fun tearDown() {
        mockBase64.close()
        mockLog.close()
    }

    private fun encoded(text: String): String =
        "base64:" + JavaBase64.getEncoder().encodeToString(text.toByteArray())

    private fun subscription(
        remarks: String = "",
        updateInterval: Long = 0L
    ): SubscriptionItem = SubscriptionItem(
        remarks = remarks,
        url = "https://example.org/sub",
        updateInterval = updateInterval
    )

    // --- имя подписки ---

    @Test
    fun `имя из заголовка подставляется вместо своего`() {
        val sub = subscription()
        SubscriptionHeaders.apply(mapOf("profile-title" to "Vanguard"), sub, remarksIsGenerated = true)
        assertEquals("Vanguard", sub.remarks)
    }

    @Test
    fun `имя из заголовка раскрывается из base64`() {
        val sub = subscription()
        SubscriptionHeaders.apply(mapOf("profile-title" to encoded("Ворота")), sub, remarksIsGenerated = true)
        assertEquals("Ворота", sub.remarks)
    }

    @Test
    fun `имя, набранное человеком, заголовок не трогает`() {
        val sub = subscription(remarks = "Мой сервис")
        SubscriptionHeaders.apply(mapOf("profile-title" to "Vanguard"), sub, remarksIsGenerated = false)
        assertEquals("Мой сервис", sub.remarks)
    }

    @Test
    fun `испорченный base64 в имени не попадает на карточку`() {
        val sub = subscription(remarks = "Прежнее")
        SubscriptionHeaders.apply(mapOf("profile-title" to "base64:!!не base64!!"), sub, remarksIsGenerated = true)
        assertEquals("Прежнее", sub.remarks)
    }

    @Test
    fun `пустое имя в заголовке ничего не стирает`() {
        val sub = subscription(remarks = "Прежнее")
        SubscriptionHeaders.apply(mapOf("profile-title" to "   "), sub, remarksIsGenerated = true)
        assertEquals("Прежнее", sub.remarks)
    }

    // --- объявление, ссылка на поддержку, значок ---

    @Test
    fun `объявление раскрывается из base64`() {
        val sub = subscription()
        SubscriptionHeaders.apply(mapOf("announce" to encoded("Профилактика в среду")), sub, true)
        assertEquals("Профилактика в среду", sub.announce)
    }

    @Test
    fun `без заголовка объявление стирается`() {
        val sub = subscription().apply { announce = "Вчерашнее" }
        SubscriptionHeaders.apply(emptyMap(), sub, true)
        assertEquals("", sub.announce)
    }

    @Test
    fun `без заголовка значок стирается`() {
        val sub = subscription().apply { icon = "https://example.org/old.png" }
        SubscriptionHeaders.apply(emptyMap(), sub, true)
        assertEquals("", sub.icon)
    }

    @Test
    fun `у значка снимаются пробелы по краям`() {
        val sub = subscription()
        SubscriptionHeaders.apply(mapOf("profile-icon" to "  https://example.org/i.png "), sub, true)
        assertEquals("https://example.org/i.png", sub.icon)
    }

    @Test
    fun `личный кабинет переносится и обрезается по краям`() {
        val sub = subscription()
        SubscriptionHeaders.apply(mapOf("profile-web-page-url" to " https://vpn.example/cab "), sub, true)
        assertEquals("https://vpn.example/cab", sub.webPageUrl)
    }

    @Test
    fun `без заголовка личный кабинет стирается`() {
        val sub = subscription().apply { webPageUrl = "https://old.example" }
        SubscriptionHeaders.apply(emptyMap(), sub, true)
        assertEquals("", sub.webPageUrl)
    }

    @Test
    fun `фирменный цвет переносится как есть`() {
        val sub = subscription()
        SubscriptionHeaders.apply(mapOf("profile-color" to " #10B981 "), sub, true)
        assertEquals("#10B981", sub.color)
    }

    @Test
    fun `без заголовка фирменный цвет стирается`() {
        val sub = subscription().apply { color = "#FF0000" }
        SubscriptionHeaders.apply(emptyMap(), sub, true)
        assertEquals("", sub.color)
    }

    @Test
    fun `ссылка на поддержку переносится как есть`() {
        val sub = subscription()
        SubscriptionHeaders.apply(mapOf("support-url" to "https://t.me/support"), sub, true)
        assertEquals("https://t.me/support", sub.supportUrl)
    }

    // --- счётчики трафика ---

    @Test
    fun `счётчики разбираются из строки панели`() {
        val sub = subscription()
        SubscriptionHeaders.apply(
            mapOf("subscription-userinfo" to "upload=1024; download=61431672324; total=0; expire=1786915378"),
            sub,
            true
        )
        assertEquals(1024L, sub.trafficUpload)
        assertEquals(61431672324L, sub.trafficDownload)
        assertEquals(0L, sub.trafficTotal)
        assertEquals(1786915378L, sub.trafficExpire)
    }

    @Test
    fun `незнакомые пары в счётчиках не мешают остальным`() {
        val sub = subscription()
        SubscriptionHeaders.apply(
            mapOf("subscription-userinfo" to "plan=premium; upload=5; download=7; total=9"),
            sub,
            true
        )
        assertEquals(5L, sub.trafficUpload)
        assertEquals(7L, sub.trafficDownload)
        assertEquals(9L, sub.trafficTotal)
    }

    @Test
    fun `нечисловое значение счётчика читается нулём, а не роняет разбор`() {
        val sub = subscription()
        SubscriptionHeaders.apply(
            mapOf("subscription-userinfo" to "upload=много; download=7"),
            sub,
            true
        )
        assertEquals(0L, sub.trafficUpload)
        assertEquals(7L, sub.trafficDownload)
    }

    @Test
    fun `без заголовка счётчики обнуляются, а не остаются от прошлой подписки`() {
        val sub = subscription().apply {
            trafficUpload = 1
            trafficDownload = 2
            trafficTotal = 3
            trafficExpire = 4
        }
        SubscriptionHeaders.apply(emptyMap(), sub, true)
        assertEquals(0L, sub.trafficUpload)
        assertEquals(0L, sub.trafficDownload)
        assertEquals(0L, sub.trafficTotal)
        assertEquals(0L, sub.trafficExpire)
    }

    // --- срок обновления ---

    @Test
    fun `часы из заголовка превращаются в минуты`() {
        val sub = subscription(updateInterval = 60L)
        val changed = SubscriptionHeaders.apply(mapOf("profile-update-interval" to "6"), sub, true)
        assertTrue(changed)
        assertEquals(360L, sub.updateInterval)
    }

    @Test
    fun `дробные часы тоже разбираются`() {
        val sub = subscription(updateInterval = 60L)
        SubscriptionHeaders.apply(mapOf("profile-update-interval" to "1.5"), sub, true)
        assertEquals(90L, sub.updateInterval)
    }

    @Test
    fun `совсем малый интервал не схлопывается в ноль`() {
        val sub = subscription(updateInterval = 60L)
        SubscriptionHeaders.apply(mapOf("profile-update-interval" to "0.001"), sub, true)
        assertEquals(1L, sub.updateInterval)
    }

    @Test
    fun `тот же интервал не считается изменением`() {
        val sub = subscription(updateInterval = 360L)
        val changed = SubscriptionHeaders.apply(mapOf("profile-update-interval" to "6"), sub, true)
        assertFalse(changed)
        assertEquals(360L, sub.updateInterval)
    }

    @Test
    fun `мусор в интервале оставляет прежний`() {
        val sub = subscription(updateInterval = 360L)
        val changed = SubscriptionHeaders.apply(mapOf("profile-update-interval" to "как-нибудь"), sub, true)
        assertFalse(changed)
        assertEquals(360L, sub.updateInterval)
    }

    @Test
    fun `отрицательный интервал не принимается`() {
        val sub = subscription(updateInterval = 360L)
        SubscriptionHeaders.apply(mapOf("profile-update-interval" to "-3"), sub, true)
        assertEquals(360L, sub.updateInterval)
    }

    // --- общее ---

    @Test
    fun `имена заголовков читаются в любом регистре`() {
        val sub = subscription()
        SubscriptionHeaders.apply(
            mapOf(
                "Profile-Title" to "Vanguard",
                "Subscription-UserInfo" to "download=7"
            ),
            sub,
            remarksIsGenerated = true
        )
        assertEquals("Vanguard", sub.remarks)
        assertEquals(7L, sub.trafficDownload)
    }
}
