package com.v2ray.ang.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * Стекло всего приложения. Своей реализации здесь больше нет: и размытие, и линзу, и
 * блики делает Kyant0/AndroidLiquidGlass - та же библиотека, на которой сделан
 * референс (Apache 2.0, условия в THIRD_PARTY.md).
 *
 * Имена и подписи оставлены прежними намеренно: стекло разошлось по двум десяткам
 * мест - карточки, меню, диалоги, поля ввода, снекбар, - и переписывать их все ради
 * смены нутра незачем. Здесь сменилось только нутро.
 */

/** Радиус размытия фона под стеклом по умолчанию. */
val GlassBlurRadius = 8.dp

/** Форма выпадающих меню. */
val GlassMenuShape = RoundedCornerShape(20.dp)

/** Форма диалогов. */
val GlassDialogShape = RoundedCornerShape(28.dp)

/**
 * Слой с содержимым экрана, который тема отдаёт всем всплывающим окнам.
 *
 * Диалоги, меню и снекбар живут в отдельных окнах и потому не попадают в запись слоя -
 * им размытие доступно. Элементам внутри самого экрана слой отсюда брать нельзя.
 */
val LocalGlassBackdrop = compositionLocalOf<GlassBackdrop?> { null }

/**
 * Снимок экрана, который стекло размывает у себя под низом.
 *
 * Обёртка над слоем библиотеки: наши экраны обращаются с ним по-своему (кто-то
 * записывает, кто-то только читает через [LocalGlassBackdrop]), и держать эту разницу
 * удобнее в своём типе.
 */
@Stable
class GlassBackdrop internal constructor(internal val layer: LayerBackdrop) {
    /** Слой в виде, понятном компонентам библиотеки. */
    val backdrop: Backdrop get() = layer
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberLayerBackdrop()
    return remember(layer) { GlassBackdrop(layer) }
}

/**
 * Пишет содержимое в [backdrop]. Вешается на корень экрана, чтобы стеклянные
 * поверхности могли размыть именно то, что под ними.
 */
@Composable
fun Modifier.glassBackdropSource(
    backdrop: GlassBackdrop,
    @Suppress("UNUSED_PARAMETER") blurRadius: Dp = GlassBlurRadius
): Modifier = layerBackdrop(backdrop.layer)

/**
 * Фон «жидкого стекла»: размытая копия того, что под элементом, преломление у края,
 * полупрозрачная тонировка и блик по контуру.
 *
 * @param shape Форма поверхности.
 * @param backdrop Слой с содержимым экрана или null, если размытия не будет.
 * @param blurRadius Радиус размытия фона.
 * @param opaqueness Плотность тонировки: 1 - как у нижней капсулы, больше - матовее.
 * @param fallbackColor Подложка, когда слоя нет.
 */
@Composable
fun Modifier.glassBackground(
    shape: Shape,
    backdrop: GlassBackdrop? = null,
    blurRadius: Dp = GlassBlurRadius,
    opaqueness: Float = 1f,
    fallbackColor: Color? = null
): Modifier {
    val scheme = MaterialTheme.colorScheme
    val isDark = LocalDarkTheme.current

    // Без слоя размывать нечего, и стекло должно быть плотнее: иначе сквозь него
    // читается то, что лежит под окном
    val solid = fallbackColor ?: scheme.surface.copy(alpha = if (isDark) 0.82f else 0.86f)
    val source = backdrop ?: return background(solid, shape)

    val tint = glassSurfaceColor(isDark, scheme.surface, opaqueness)

    return drawBackdrop(
        backdrop = source.backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurRadius.toPx())
            // Преломление у края - то, чем стекло отличается от простого размытия
            lens(12f.dp.toPx(), 24f.dp.toPx(), chromaticAberration = true)
        },
        highlight = { Highlight.Ambient },
        shadow = { Shadow(radius = 8f.dp, color = Color.Black.copy(alpha = 0.08f)) },
        onDrawSurface = { drawRect(tint) }
    )
}

/**
 * Стекло для окон поверх экрана - диалогов, меню, шторок, снекбара. Слой берётся из
 * темы, так что ставить его вручную не нужно.
 */
@Composable
fun Modifier.glassPanel(
    shape: Shape,
    blurRadius: Dp = GlassBlurRadius,
    opaqueness: Float = 0.7f,
    fallbackColor: Color? = null
): Modifier {
    val dense = fallbackColor ?: MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)
    return glassBackground(
        shape = shape,
        backdrop = LocalGlassBackdrop.current,
        blurRadius = blurRadius,
        opaqueness = opaqueness,
        fallbackColor = dense
    )
}

/**
 * Стеклянная поверхность с содержимым. Параметры - как у [glassBackground].
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    backdrop: GlassBackdrop? = null,
    blurRadius: Dp = GlassBlurRadius,
    opaqueness: Float = 1f,
    fallbackColor: Color? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier.glassBackground(
            shape = shape,
            backdrop = backdrop,
            blurRadius = blurRadius,
            opaqueness = opaqueness,
            fallbackColor = fallbackColor
        ),
        content = content
    )
}

/**
 * Плёнка поверх стекла. Раньше это был градиент, теперь ровный тон: блик по контуру
 * рисует библиотека, и второй сверху только мутил картину.
 */
fun glassSurfaceColor(isDark: Boolean, surface: Color, opaqueness: Float = 1f): Color {
    val alpha = if (isDark) 0.12f else 0.28f
    val base = if (isDark) Color.White else surface
    return base.copy(alpha = (alpha * opaqueness).coerceIn(0f, 1f))
}
