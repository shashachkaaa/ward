package com.v2ray.ang.handler

import android.util.Base64
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.util.LogUtil

/**
 * Заголовки ответа подписки: имя, объявление, значок, лимиты, срок обновления.
 *
 * Вынесено из обновления подписки отдельно и без сети, потому что ломается это чаще
 * всего не у нас. Панель на той стороне живёт своей жизнью: меняет формат счётчиков,
 * начинает слать имя в base64, отдаёт интервал дробным числом. Заметить такое по
 * работающему приложению трудно - подписка обновилась, конфиги приехали, а лимит
 * молча обнулился. Здесь это проверяется тестами, без панели и без устройства.
 *
 * Ключи ожидаются в нижнем регистре - имена заголовков в HTTP регистронезависимы,
 * и приводит их к одному виду тот, кто читает ответ. Здесь на это не полагаемся:
 * перепутать легко, а увидеть - нет.
 */
object SubscriptionHeaders {

    /** Значение, которое панель может прислать закодированным. */
    private const val BASE64_PREFIX = "base64:"

    /**
     * Переносит заголовки в запись подписки.
     *
     * Отсутствие заголовка - это тоже значение: объявление, значок и счётчики
     * стираются. Иначе они остались бы от прошлого ответа, и человек читал бы
     * позавчерашнее объявление или лимит от другой подписки.
     *
     * @param headers Заголовки ответа.
     * @param subscription Запись, которую правим на месте.
     * @param remarksIsGenerated Своё ли имя у подписки. Имя, набранное человеком,
     *   заголовок не перебивает - иначе оно слетало бы при каждом обновлении.
     * @return Изменился ли срок обновления: планировщику нужно знать об этом,
     *   чтобы переставить задачу.
     */
    fun apply(
        headers: Map<String, String>,
        subscription: SubscriptionItem,
        remarksIsGenerated: Boolean
    ): Boolean {
        val values = headers.mapKeys { it.key.lowercase() }

        applyTitle(values["profile-title"], subscription, remarksIsGenerated)
        subscription.announce = decode(values["announce"], "announce").orEmpty()
        subscription.supportUrl = values["support-url"].orEmpty()
        // Значок храним как пришёл - ссылкой или картинкой. Разбирает и проверяет
        // его тот, кто рисует
        subscription.icon = values["profile-icon"]?.trim().orEmpty()

        applyUserInfo(values["subscription-userinfo"], subscription)
        return applyUpdateInterval(values["profile-update-interval"], subscription)
    }

    private fun applyTitle(
        raw: String?,
        subscription: SubscriptionItem,
        remarksIsGenerated: Boolean
    ) {
        if (!remarksIsGenerated) return
        val title = decode(raw, "profile-title")?.takeIf { it.isNotBlank() } ?: return
        subscription.remarks = title
    }

    /**
     * Срок обновления приходит в часах, а храним мы его в минутах. Дробные значения
     * встречаются, поэтому читаем числом с плавающей точкой; меньше минуты не бывает.
     */
    private fun applyUpdateInterval(raw: String?, subscription: SubscriptionItem): Boolean {
        if (raw.isNullOrBlank()) return false

        val hours = raw.trim().toDoubleOrNull()
        if (hours == null || hours <= 0) {
            LogUtil.w(AppConfig.TAG, "Bad profile-update-interval header: $raw")
            return false
        }

        val minutes = (hours * 60).toLong().coerceAtLeast(1L)
        if (minutes == subscription.updateInterval) return false

        subscription.updateInterval = minutes
        LogUtil.i(AppConfig.TAG, "Subscription update interval from header: $hours h")
        return true
    }

    /**
     * Счётчики приходят одной строкой: `upload=0; download=61431672324; total=0; expire=1786915378`.
     *
     * Незнакомые пары пропускаем, а не считаем ответ испорченным: панель вправе
     * дописать в эту строку что-то своё, и ронять из-за этого весь разбор незачем.
     */
    private fun applyUserInfo(raw: String?, subscription: SubscriptionItem) {
        if (raw.isNullOrEmpty()) {
            subscription.trafficUpload = 0L
            subscription.trafficDownload = 0L
            subscription.trafficTotal = 0L
            subscription.trafficExpire = 0L
            return
        }

        raw.split(";").forEach { part ->
            val key = part.substringBefore('=', "").trim().lowercase()
            if (key.isEmpty() || '=' !in part) return@forEach
            val value = part.substringAfter('=').trim().toLongOrNull() ?: 0L
            when (key) {
                "upload" -> subscription.trafficUpload = value
                "download" -> subscription.trafficDownload = value
                "total" -> subscription.trafficTotal = value
                "expire" -> subscription.trafficExpire = value
            }
        }
    }

    /**
     * Раскрывает значение, если панель прислала его закодированным.
     *
     * Не раскрылось - значит значения нет. Раньше в таком случае бралась исходная
     * строка, и в названии подписки оказывалось «base64:» с мусором за ним: показать
     * такое хуже, чем не показать ничего.
     */
    private fun decode(raw: String?, what: String): String? {
        if (raw.isNullOrEmpty()) return null
        if (!raw.startsWith(BASE64_PREFIX)) return raw

        return try {
            String(
                Base64.decode(raw.substringAfter(BASE64_PREFIX), Base64.DEFAULT),
                Charsets.UTF_8
            )
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to decode base64 $what", e)
            null
        }
    }
}
