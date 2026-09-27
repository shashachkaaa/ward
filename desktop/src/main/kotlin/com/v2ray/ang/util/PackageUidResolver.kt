package com.v2ray.ang.util

import android.content.Context

/**
 * Настольная версия: правила по приложениям работают только в туннеле, которого
 * здесь пока нет, и [com.v2ray.ang.handler.SettingsManager.canUseProcessRouting]
 * всегда отвечает «нет». Остаётся, чтобы сборка конфига осталась копией.
 */
object PackageUidResolver {
    @Suppress("UNUSED_PARAMETER")
    fun packageNamesToUids(context: Context, packageNames: List<String>): List<String> = emptyList()
}
