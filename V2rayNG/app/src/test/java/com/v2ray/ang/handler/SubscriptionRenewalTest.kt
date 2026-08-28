package com.v2ray.ang.handler

import com.v2ray.ang.dto.entities.SubscriptionItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Порог, по которому подписку пора продлевать.
 *
 * Он один на двоих: по нему уходит предупреждение в шторку и по нему же появляется
 * кнопка на карточке. Разойдись они - человек получил бы «осталось 8%» уведомлением
 * и карточку без кнопки, то есть совет без способа ему последовать. Поэтому порог
 * закреплён здесь, а не проверяется глазами на устройстве.
 */
class SubscriptionRenewalTest {

    private fun daysFromNow(days: Long): Long =
        (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days)) / 1000

    private fun subscription(
        expireSeconds: Long = 0L,
        total: Long = 0L,
        used: Long = 0L
    ): SubscriptionItem = SubscriptionItem(
        remarks = "test",
        url = "https://example.org/sub",
        trafficExpire = expireSeconds,
        trafficTotal = total,
        trafficDownload = used
    )

    @Test
    fun `бессрочная подписка без лимита продлевать не просит`() {
        assertFalse(SubscriptionAlerts.needsRenewal(subscription()))
    }

    @Test
    fun `далёкий срок продлевать не просит`() {
        assertFalse(SubscriptionAlerts.needsRenewal(subscription(expireSeconds = daysFromNow(30))))
    }

    @Test
    fun `срок на исходе просит продлить`() {
        assertTrue(SubscriptionAlerts.needsRenewal(subscription(expireSeconds = daysFromNow(2))))
    }

    @Test
    fun `истёкший срок просит продлить`() {
        assertTrue(SubscriptionAlerts.needsRenewal(subscription(expireSeconds = daysFromNow(-5))))
    }

    @Test
    fun `срок в миллисекундах понимается наравне с секундами`() {
        val millis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2)
        assertTrue(SubscriptionAlerts.needsRenewal(subscription(expireSeconds = millis)))
    }

    @Test
    fun `половина трафика продлевать не просит`() {
        assertFalse(SubscriptionAlerts.needsRenewal(subscription(total = 100, used = 50)))
    }

    @Test
    fun `девяносто процентов трафика просит продлить`() {
        assertTrue(SubscriptionAlerts.needsRenewal(subscription(total = 100, used = 90)))
    }

    @Test
    fun `исчерпанный трафик просит продлить`() {
        assertTrue(SubscriptionAlerts.needsRenewal(subscription(total = 100, used = 120)))
    }

    @Test
    fun `без лимита израсходованное не считается`() {
        assertFalse(SubscriptionAlerts.needsRenewal(subscription(total = 0, used = 999999)))
    }
}
