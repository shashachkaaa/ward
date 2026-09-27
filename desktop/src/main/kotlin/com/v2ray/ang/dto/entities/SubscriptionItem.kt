package com.v2ray.ang.dto.entities

data class SubscriptionItem(
    var remarks: String = "",
    var url: String = "",
    var enabled: Boolean = true,
    val addedTime: Long = System.currentTimeMillis(),
    var lastUpdated: Long = -1,
    var autoUpdate: Boolean = true,
    var updateInterval: Long = 1440,
    var prevProfile: String? = null,
    var nextProfile: String? = null,
    var filter: String? = null,
    var allowInsecureUrl: Boolean = false,
    var userAgent: String? = null,
    var requestHeaders: String? = null,
    var announce: String = "",
    var supportUrl: String = "",
    /**
     * Значок сервиса из заголовка `profile-icon`: ссылка или `base64:` с самой
     * картинкой. Хранится как пришло - разбирается при отрисовке.
     */
    var icon: String = "",
    /**
     * Личный кабинет из заголовка `profile-web-page-url`: там продлевают подписку.
     * Пусто - кнопки продления не будет, вести некуда.
     */
    var webPageUrl: String = "",
    /**
     * Фирменный цвет сервиса из заголовка `profile-color`, как пришёл. Приводится
     * к палитре при отрисовке: владельцу достаётся тон, светлота остаётся нашей.
     */
    var color: String = "",
    var trafficUpload: Long = 0L,
    var trafficDownload: Long = 0L,
    var trafficTotal: Long = 0L,
    var trafficExpire: Long = 0L
)
