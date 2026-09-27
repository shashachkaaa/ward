package com.v2ray.ang.ui.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.util.lerp

/**
 * Живой фон экрана.
 *
 * Раньше под всем лежала сплошная заливка, и это било дважды. Экран со сплошным фоном
 * читается как заготовка - так выглядит пустой шаблон, а не приложение. И, что важнее,
 * вхолостую работало всё стекло: линза преломляет то, что под ней, а под ней ровный
 * цвет - гнуть нечего. Отсюда и приходилось давать чипам объём светом вместо
 * преломления, и полоса размытия внизу размывала однотонное поле.
 *
 * Здесь - несколько очень размытых пятен акцента по краям. Они дают стеклу фактуру,
 * которую есть смысл преломлять, и при этом не лезут в глаза.
 *
 * Движения нет намеренно. Экран целиком пишется в слой для стекла, и любое непрерывное
 * шевеление фона заставляло бы переписывать этот слой каждый кадр даже в покое.
 * Меняется только накал - по состоянию подключения, то есть тогда, когда это что-то
 * значит.
 *
 * На тёмной теме пятна нарочно слабые и прижаты к краям: там фон чёрный, и берут
 * приложение в том числе ради чёрного на AMOLED - заливать его цветом нельзя.
 *
 * @param backdrop Слой, в который фон записывается отдельно от остального экрана.
 *   Его берёт стекло, живущее внутри содержимого: карточки, поля, таблетки. Общий слой
 *   им недоступен - они сами в него пишутся, а рисовать слой внутри его же записи
 *   нельзя. И преломлять карточке разумно именно фон, а не соседние карточки.
 * @param activity 0 - покой, 1 - соединение установлено. Лямбдой, а не значением:
 *   читается в отрисовке, и накал не тянет за собой перекомпоновку.
 */
@Composable
fun Modifier.liquidBackground(
    backdrop: GlassBackdrop?,
    activity: () -> Float
): Modifier {
    val scheme = MaterialTheme.colorScheme
    val isDark = LocalDarkTheme.current

    val base = scheme.background
    val warm = scheme.primary
    val cool = scheme.tertiary

    // Насколько пятна вообще заметны. На чёрном фоне тот же уровень, что на светлом,
    // выглядел бы грязью
    val idleAlpha = if (isDark) 0.10f else 0.16f
    val liveAlpha = if (isDark) 0.20f else 0.28f

    val draw: DrawScope.(alpha: Float) -> Unit = { alpha ->
        drawRect(base)
        // Сверху слева - основное пятно акцента
        blob(warm, alpha, Offset(size.width * 0.12f, size.height * 0.04f), size.width * 0.95f)
        // Справа, ниже кнопки - второй тон, чтобы фон не был одноцветным
        blob(cool, alpha * 0.75f, Offset(size.width * 1.02f, size.height * 0.26f), size.width * 0.85f)
        // Снизу - под нижней капсулой: ей есть что размывать, а полосе затухания есть
        // что растворять
        blob(warm, alpha * 0.6f, Offset(size.width * 0.5f, size.height * 1.04f), size.width * 0.9f)
    }

    // Без слоя - и когда его некому дать, и когда стекло выключено: слой нужен
    // только тому, кто фон преломляет, а на выключенном стекле таких нет
    if (backdrop == null || !LocalGlassQuality.current.blurs) {
        return drawBehind { draw(lerp(idleAlpha, liveAlpha, activity().coerceIn(0f, 1f))) }
    }

    // Фон неподвижен, а перерисовывался каждый кадр: три радиальных градиента во весь
    // экран, и каждый заново создавал свой шейдер. На прокрутке это ложилось поверх
    // всей остальной работы. Теперь картинка пишется в слой только когда меняется -
    // при смене размера, накала или цветов, - а каждый кадр слой просто выкладывается
    // на экран
    val cache = remember(backdrop) { BackgroundCache() }

    return this
        .onGloballyPositioned { backdrop.origin = it.positionOnScreen() }
        .drawBehind {
            val alpha = lerp(idleAlpha, liveAlpha, activity().coerceIn(0f, 1f))
            if (cache.needsRedraw(size, alpha, base, warm, cool)) {
                backdrop.layer.record { draw(alpha) }
                backdrop.recorded = true
                cache.remember(size, alpha, base, warm, cool)
            }
            drawLayer(backdrop.layer)
        }
}

/**
 * Что уже записано в слой фона: пока это не изменилось, перезаписывать нечего.
 *
 * Цвета здесь не для красоты. Первый заход сравнивал только размер и накал, и смена
 * акцента в настройках до фона не доходила: размер тот же, накал тот же - значит
 * перерисовывать нечего. Пятна оставались прежнего цвета до перезапуска, пока весь
 * остальной экран уже был перекрашен.
 */
private class BackgroundCache {
    private var size: Size = Size.Unspecified
    private var alpha: Float = Float.NaN
    private var base: Color = Color.Unspecified
    private var warm: Color = Color.Unspecified
    private var cool: Color = Color.Unspecified

    fun needsRedraw(size: Size, alpha: Float, base: Color, warm: Color, cool: Color): Boolean =
        this.size != size ||
                this.alpha != alpha ||
                this.base != base ||
                this.warm != warm ||
                this.cool != cool

    fun remember(size: Size, alpha: Float, base: Color, warm: Color, cool: Color) {
        this.size = size
        this.alpha = alpha
        this.base = base
        this.warm = warm
        this.cool = cool
    }
}

/** Одно размытое пятно: цвет в центре, прозрачность к краю. */
private fun DrawScope.blob(color: Color, alpha: Float, center: Offset, radius: Float) {
    if (alpha <= 0f || radius <= 0f) return
    drawRect(
        Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius
        )
    )
}
