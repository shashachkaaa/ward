package com.v2ray.ang.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
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
 * Слой, в который записан только фон экрана - без содержимого.
 *
 * Нужен стеклу, которое живёт внутри содержимого: карточкам, полям, таблеткам.
 * Общий слой им брать нельзя - они сами в него пишутся, а рисовать слой внутри его
 * же записи запрещено. В этот слой они не попадают, поэтому его можно рисовать
 * свободно; и преломлять карточке разумно именно фон, а не соседние карточки.
 */
val LocalContentBackdrop = compositionLocalOf<GlassBackdrop?> { null }

/**
 * Снимок экрана, который стекло размывает у себя под низом.
 *
 * Свой, а не библиотечный `LayerBackdrop`: тот считает смещение в оконных
 * координатах (`positionInWindow`), а диалоги, меню и снекбар живут каждый в своём
 * окне со своим началом отсчёта. Смещение выходило неверным, и стекло брало кусок
 * экрана не из-под себя - размывалось то, чего под окном нет. Экранные координаты
 * общие для всех окон, и с ними такого не бывает.
 */
@Stable
class GlassBackdrop internal constructor(internal val layer: GraphicsLayer) : Backdrop {

    override val isCoordinatesDependent: Boolean = true

    /** Левый верхний угол записанного содержимого в координатах экрана. */
    internal var origin by mutableStateOf(Offset.Zero)

    /**
     * Записан ли слой хоть раз. На выключенном стекле экран в слой не пишется вовсе,
     * и рисовать пустой слой нечего - а спросить об этом больше некого.
     */
    internal var recorded = false

    /** Он же в виде, понятном компонентам библиотеки. */
    val backdrop: Backdrop get() = this

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        if (!recorded) return
        val here = coordinates?.positionOnScreen() ?: return
        translate(left = origin.x - here.x, top = origin.y - here.y) {
            drawLayer(layer)
        }
    }
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

/**
 * Пишет содержимое в [backdrop] и тут же рисует его на экране. Вешается на корень
 * экрана, чтобы стеклянные поверхности могли размыть именно то, что под ними.
 *
 * На выключенном стекле не делает ничего. Это не мелочь: запись всего экрана в
 * отдельный слой заставляет видеоядро каждый кадр рисовать в буфер, а потом
 * выкладывать его на экран - вторая полная отрисовка поверх первой. Читать этот слой
 * при выключенном стекле всё равно некому.
 */
@Composable
fun Modifier.glassBackdropSource(
    backdrop: GlassBackdrop,
    @Suppress("UNUSED_PARAMETER") blurRadius: Dp = GlassBlurRadius
): Modifier {
    if (!LocalGlassQuality.current.blurs) return this
    return this
        .onGloballyPositioned { backdrop.origin = it.positionOnScreen() }
        .drawWithContent {
            backdrop.layer.record { this@drawWithContent.drawContent() }
            backdrop.recorded = true
            drawLayer(backdrop.layer)
        }
}

/**
 * Фон «жидкого стекла»: размытая копия того, что под элементом, преломление у края,
 * полупрозрачная тонировка и блик по контуру.
 *
 * @param shape Форма поверхности.
 * @param backdrop Слой с содержимым экрана или null, если размытия не будет.
 * @param blurRadius Радиус размытия фона.
 * @param opaqueness Плотность тонировки: 1 - как у нижней капсулы, больше - матовее.
 * @param dispersion Разложение по краю на цветные каёмки. Красиво на крупном стекле,
 *   но это отдельный, заметно более тяжёлый шейдер: на мелких поверхностях, которых
 *   на экране много, его лучше не включать.
 * @param surfaceTint Свой цвет поверх стекла. Кладётся здесь, а не фоном содержимого:
 *   фон содержимого формой стекла не обрезается и лёг бы поверх него прямоугольником.
 * @param fallbackColor Подложка, когда слоя нет.
 */
@Composable
fun Modifier.glassBackground(
    shape: Shape,
    backdrop: GlassBackdrop? = null,
    blurRadius: Dp = GlassBlurRadius,
    opaqueness: Float = 1f,
    surfaceTint: Color? = null,
    dispersion: Boolean = true,
    fallbackColor: Color? = null
): Modifier {
    val scheme = MaterialTheme.colorScheme
    val isDark = LocalDarkTheme.current
    val quality = LocalGlassQuality.current

    // Без слоя размывать нечего, и поверхность должна быть плотнее: иначе сквозь неё
    // читается то, что лежит под окном
    val solid = fallbackColor ?: scheme.surface.copy(alpha = if (isDark) 0.82f else 0.86f)
    val source = backdrop?.takeIf { quality.blurs }

    if (source == null) {
        // Слоя нет - но край всё равно держим светом. Ровная заливка отличается от
        // фона только тоном, и на широкой карточке такое отличие читается чертой
        // поперёк экрана, а не поверхностью.
        //
        // Свет рисуется обводкой, а не бликом библиотеки. Блик у неё - размытие
        // контура по слою во всю поверхность, а поверхности тут бывают ростом со
        // всё содержимое экрана: в разделе настроек одна карточка оборачивает всю
        // прокручиваемую колонку. Размывать контур по такому слою каждый кадр -
        // это и есть те самые рывки. Обводка же просто линия.
        //
        // Свет падает сверху, поэтому и гаснет он книзу - и гаснет на своей высоте,
        // не растягиваясь по всей карточке, какой бы длинной она ни была
        val rim = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) 0.12f else 0.60f),
                Color.White.copy(alpha = if (isDark) 0.04f else 0.16f),
                Color.Transparent
            ),
            startY = 0f,
            endY = with(LocalDensity.current) { 56.dp.toPx() }
        )
        return background(solid, shape).border(1.dp, rim, shape)
    }

    val tint = glassSurfaceColor(isDark, scheme.surface, opaqueness)

    return drawBackdrop(
        backdrop = source.backdrop,
        shape = { shape },
        effects = {
            // Стекло подстраивается под яркость того, что под ним. Мерить её нечем и
            // незачем: под стеклом всегда фон приложения, а он задан темой. На тёмной
            // размытая копия уходит в ровное серое пятно - её надо чуть поднять и
            // растянуть по контрасту, на светлой наоборот приглушить, иначе стекло
            // выцветает в белое. Оба набора - это colorControls, у которого saturation
            // 1.5 и есть тот самый vibrancy
            if (isDark) {
                colorControls(brightness = 0.06f, contrast = 1.12f, saturation = 1.5f)
            } else {
                colorControls(brightness = -0.03f, contrast = 1.06f, saturation = 1.5f)
            }
            blur(blurRadius.toPx())
            // Преломление у края - то, чем стекло отличается от простого размытия.
            // Оно же и самое дорогое: отдельная программа для видеоядра на каждую
            // поверхность каждый кадр. На упрощённом уровне его нет
            if (quality.refracts) {
                lens(12f.dp.toPx(), 24f.dp.toPx(), chromaticAberration = dispersion)
            }
        },
        highlight = { Highlight.Ambient },
        shadow = { Shadow(radius = 8f.dp, color = Color.Black.copy(alpha = 0.08f)) },
        onDrawSurface = {
            drawRect(tint)
            // Библиотека обрезает эту заливку формой стекла, поэтому цвет ложится
            // капсулой или скруглённым прямоугольником, а не квадратом
            if (surfaceTint != null) drawRect(surfaceTint)
        }
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
    surfaceTint: Color? = null,
    dispersion: Boolean = true,
    fallbackColor: Color? = null,
    border: Color? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .glassBackground(
            shape = shape,
            backdrop = backdrop,
            blurRadius = blurRadius,
            opaqueness = opaqueness,
            surfaceTint = surfaceTint,
            dispersion = dispersion,
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
