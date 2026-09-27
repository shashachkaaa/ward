package com.ward.desktop.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/*
 * Цвет сервиса из заголовка profile-color - как в AccentColor.kt на Android:
 * от присланного цвета берётся только тон, светлоту и насыщенность задаём мы.
 * ColorUtils из androidx здесь нет, поэтому перевод в HSL и обратно свой.
 */

fun serviceColor(raw: String, dark: Boolean): Color? {
    val hex = raw.trim().removePrefix("#")
    val full = when (hex.length) {
        3 -> hex.map { "$it$it" }.joinToString("")
        6 -> hex
        8 -> hex.takeLast(6)
        else -> return null
    }
    val rgb = full.toLongOrNull(16)?.toInt() ?: return null
    val seed = Color(0xFF000000.toInt() or rgb)
    return if (dark) seed.tone(0.72f, 0.9f) else seed.tone(0.45f)
}

private fun Color.tone(lightness: Float, saturation: Float = 1f): Color {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val d = max - min
    val l = (max + min) / 2f
    val s = if (d == 0f) 0f else d / (1f - abs(2f * l - 1f))
    val h = when {
        d == 0f -> 0f
        max == red -> 60f * (((green - blue) / d).mod(6f))
        max == green -> 60f * (((blue - red) / d) + 2f)
        else -> 60f * (((red - green) / d) + 4f)
    }
    return Color.hsl(h, (s * saturation).coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))
}
