package com.v2ray.ang.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/*
 * Тема настольной версии. Палитры скопированы из Theme.kt приложения под Android
 * как есть; своего здесь только сборка темы - без цветов из обоев и настроек окна.
 */

internal val LightColor = lightColorScheme(
    primary = Color(0xFF4F46E5), // Indigo - акцент интерфейса
    onPrimary = Color(0xFFFFFFFF), // White
    primaryContainer = Color(0xFFE6E4FF), // Pale Indigo
    onPrimaryContainer = Color(0xFF1B1663), // Deep Indigo
    secondary = Color(0xFFf97910), // Orange
    onSecondary = Color(0xFFFFFFFF), // White
    secondaryContainer = Color(0xFFFFE8D6), // Pale Orange
    onSecondaryContainer = Color(0xFF2B1700), // Dark Brown
    tertiary = Color(0xFF009966), // Green
    onTertiary = Color(0xFFFFFFFF), // White
    tertiaryContainer = Color(0xFFA0F2D0), // Light Green
    onTertiaryContainer = Color(0xFF00201A), // Dark Teal
    error = Color(0xFFBA1A1A), // Red
    errorContainer = Color(0xFFFFDAD6), // Light Red
    onError = Color(0xFFFFFFFF), // White
    onErrorContainer = Color(0xFF410002), // Dark Red
    background = Color(0xFFF6F6FB), // Мягкий холодный фон, чтобы карточки читались
    onBackground = Color(0xFF17161C), // Near Black
    surface = Color(0xFFFFFFFF), // White
    onSurface = Color(0xFF17161C), // Near Black
    surfaceVariant = Color(0xFFE6E4F0), // Light Indigo Gray
    onSurfaceVariant = Color(0xFF585563), // Muted Gray
    outline = Color(0xFF8B8898), // Medium Gray
    outlineVariant = Color(0xFFD5D2DF), // Light Gray
    inverseSurface = Color(0xFF313033), // Dark Gray
    inverseOnSurface = Color(0xFFF4EFF4), // Very Light Gray
    inversePrimary = Color(0xFFC2BFFF), // Light Indigo
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFF4F46E5), // Indigo
    surfaceContainerLowest = Color(0xFFFFFFFF), // White
    surfaceContainerLow = Color(0xFFF3F2F9), // Very Light Indigo Gray
    surfaceContainer = Color(0xFFEDECF5), // Light Indigo Gray
    surfaceContainerHigh = Color(0xFFE7E5F1), // Light Indigo Gray
    surfaceContainerHighest = Color(0xFFDFDDED), // Light Indigo Gray
)

internal val DarkColor = darkColorScheme(
    primary = Color(0xFFA5A2FF), // Light Indigo - акцент интерфейса
    onPrimary = Color(0xFF1E1B54), // Deep Indigo
    primaryContainer = Color(0xFF322E7A), // Indigo
    onPrimaryContainer = Color(0xFFE4E1FF), // Pale Indigo
    secondary = Color(0xFFf97910), // Orange
    onSecondary = Color(0xFF4E2600), // Dark Brown
    secondaryContainer = Color(0xFF6F3800), // Brown
    onSecondaryContainer = Color(0xFFFFE8D6), // Pale Orange
    tertiary = Color(0xFF83D6B5), // Mint Green
    onTertiary = Color(0xFF00382E), // Dark Teal
    tertiaryContainer = Color(0xFF005143), // Teal
    onTertiaryContainer = Color(0xFFA0F2D0), // Light Green
    error = Color(0xFFFFB4AB), // Light Red
    errorContainer = Color(0xFF93000A), // Dark Red
    onError = Color(0xFF690005), // Deep Red
    onErrorContainer = Color(0xFFFFDAD6), // Light Red
    
    // --- ИЗМЕНЕНИЯ ДЛЯ AMOLED НИЖЕ ---
    background = Color(0xFF000000), // Pure Black (вместо 0xFF1C1B1F)
    onBackground = Color(0xFFE6E1E5), // Light Gray
    surface = Color(0xFF000000), // Pure Black (вместо 0xFF1C1B1F)
    onSurface = Color(0xFFE6E1E5), // Light Gray
    surfaceVariant = Color(0xFF49454F), // Dark Gray
    onSurfaceVariant = Color(0xFFCAC4D0), // Light Gray
    outline = Color(0xFF938F99), // Grayish Purple
    outlineVariant = Color(0xFF49454F), // Dark Gray
    inverseSurface = Color(0xFFE6E1E5), // Light Gray
    inverseOnSurface = Color(0xFF000000), // Pure Black (вместо 0xFF1C1B1F)
    inversePrimary = Color(0xFF4F46E5), // Indigo
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFFA5A2FF), // Light Indigo
    
    // Затемняем контейнеры. Можно оставить легкий серый оттенок для High/Highest, 
    // чтобы карточки не сливались в единое пятно, но Lowest, Low и базовый делаем черными.
    surfaceContainerLowest = Color(0xFF000000), // Pure Black
    surfaceContainerLow = Color(0xFF000000), // Pure Black
    surfaceContainer = Color(0xFF000000), // Pure Black
    surfaceContainerHigh = Color(0xFF121212), // Very Dark Gray (для контраста элементов)
    surfaceContainerHighest = Color(0xFF1E1E1E), // Dark Gray (для верхних карточек)
)

// Semantic Colors
val colorPing = Color(0xFF009966) // Green
val colorPingRed = Color(0xFFFF0099) // Pink Red
val colorConfigType = Color(0xFFf97910) // Orange
val colorFabActive = Color(0xFFf97910) // Orange
val colorFabInactiveLight = Color(0xFF9C9C9C) // Gray
val colorFabInactiveDark = Color(0xFF646464) // Dark Gray
val dividerColorLight = Color(0xFFE0E0E0) // Light Gray
val dividerColorDark = Color(0xFF424242) // Dark Gray

// Toast Colors 70%
val toastNormalBgLight = Color(0xB3353A3E) // Dark Gray
val toastNormalBgDark = Color(0xB34A4F54) // Darker Gray
val toastSuccessBg = Color(0xB3388E3C) // Green
val toastErrorBg = Color(0xB3D50000) // Red
val toastInfoBg = Color(0xB33F51B5) // Indigo Blue
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White
val colorPingSlow = Color(0xFFFFA500) // Orange

val LocalDarkTheme = compositionLocalOf { false }

/** Разрешено ли красить карточки в фирменные цвета сервисов. */
val LocalServiceColors = compositionLocalOf { true }

/**
 * Уровень стекла. На компьютере видеоядро стекло тянет всегда, поэтому уровень
 * один - полный; перечисление оставлено, чтобы скопированное стекло не менять.
 */
enum class GlassQuality {
    FULL, LITE, OFF;

    val blurs: Boolean get() = this != OFF
    val refracts: Boolean get() = this == FULL
    val isAdaptive: Boolean get() = false
    fun movingOrLess(): GlassQuality = this
}

val LocalGlassQuality = compositionLocalOf { GlassQuality.FULL }
val LocalGlassAdaptive = compositionLocalOf { false }

@Composable
fun DesktopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val backdrop = rememberGlassBackdrop()
    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalGlassBackdrop provides backdrop,
        LocalGlassQuality provides GlassQuality.FULL,
    ) {
        MaterialTheme(colorScheme = if (darkTheme) DarkColor else LightColor) {
            Box(Modifier.fillMaxSize().glassBackdropSource(backdrop)) {
                content()
            }
        }
    }
}

/**
 * Угол блика на стекле. На телефоне его ведёт датчик наклона; у монитора наклона
 * нет, и свет стоит там, где он у телефона, который держат прямо.
 */
@Composable
fun rememberGravityAngle(): androidx.compose.runtime.State<Float> =
    androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(45f) }
