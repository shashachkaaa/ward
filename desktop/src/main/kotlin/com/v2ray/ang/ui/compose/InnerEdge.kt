package com.v2ray.ang.ui.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Свет и тень по внутренней кромке стеклянной поверхности.
 *
 * Своё, а не внутренняя тень библиотеки, и причин тому две.
 *
 * Первая - смысл. Тень у неё именно тень: смещение по умолчанию равно радиусу, и
 * падает она с одной стороны. Ровного отсвета со всех сторон из неё не выходит.
 *
 * Вторая - она ломается. Тень держит свой слой видеоядра и запомненный радиус
 * размытия, а слой при переиспользовании строки списка создаётся заново - радиус же
 * остаётся прежним, и размытие новому слою уже не назначается. Первый показ мягкий,
 * после прокрутки вниз и обратно - резкая полоса. На карточке подписки это было
 * видно во всю ширину; на кнопке помельче, но родом оттуда же.
 *
 * Здесь слоя нет вовсе: градиент и обрезка по форме. Переиспользовать нечего,
 * значит и портиться нечему.
 */

/** Насколько далеко от края внутрь заходит отсвет. */
private val DefaultDepth = 20.dp

/**
 * Отсвет цвета внутрь от всех четырёх краёв: у кромки цвет виден, к середине гаснет.
 *
 * Рисуется не четырьмя полосами по сторонам, а обводками самой формы. Полосы -
 * это прямоугольник: внутренний край отсвета выходил с острыми углами, а у
 * скругления карточки отсвет обрезался и пропадал, и цвет читался квадратной
 * рамкой внутри круглой. Обводка идёт по контуру формы, поэтому и отсвет
 * повторяет её скругление.
 *
 * Мягкость - из стопки обводок: каждая следующая шире и заходит глубже, а
 * прозрачны они так, что вместе у самой кромки дают исходный цвет. Там, где
 * лежат все обводки, цвет плотнее всего; к середине их всё меньше, и он гаснет.
 * Обрезка по форме оставляет от каждой только внутреннюю половину.
 */
fun Modifier.innerEdgeGlow(color: Color, shape: Shape, depth: Dp = DefaultDepth): Modifier =
    drawWithCache {
        val inset = depth.toPx().coerceAtMost(size.minDimension / 2f)
        if (inset <= 0f || color.alpha <= 0f) return@drawWithCache onDrawBehind {}

        val outline = clipPathOf(shape)
        // Прозрачность одного слоя подобрана так, чтобы все слои вместе давали
        // у кромки ровно исходную: 1 - (1 - a)^n = alpha
        val layerAlpha = 1f - Math.pow(1.0 - color.alpha, 1.0 / GlowLayers).toFloat()
        val layer = color.copy(alpha = layerAlpha)
        val strokes = List(GlowLayers) { i ->
            // Обводка ложится по контуру поровну наружу и внутрь, а внутрь нужно
            // заглянуть на всю глубину слоя - отсюда удвоение
            Stroke(width = 2f * inset * (i + 1) / GlowLayers)
        }

        onDrawBehind {
            clipPath(outline) {
                strokes.forEach { drawPath(outline, layer, style = it) }
            }
        }
    }

/** Сколько обводок складывают отсвет: меньше - видны ступени, больше - лишняя работа. */
private const val GlowLayers = 8

/**
 * Тёмная кромка внутри по верхнему краю - толщина стенки у стекла.
 *
 * Только сверху: это не отсвет, а как раз тень, и падает она с той стороны, куда
 * свет не достаёт.
 */
fun Modifier.innerEdgeShade(color: Color, shape: Shape, depth: Dp): Modifier =
    drawWithCache {
        val inset = depth.toPx().coerceAtMost(size.height / 2f)
        if (inset <= 0f) return@drawWithCache onDrawBehind {}

        val clip = clipPathOf(shape)
        val shade = Brush.verticalGradient(listOf(color, color.fadedOut()), 0f, inset)

        onDrawBehind {
            clipPath(clip) {
                drawRect(shade, size = Size(size.width, inset))
            }
        }
    }

/**
 * Прозрачный конец градиента берётся у самого цвета, а не у [Color.Transparent]: тот
 * прозрачно чёрный, и градиент к нему уходил бы через грязно-серое, а не просто гас.
 */
private fun Color.fadedOut(): Color = copy(alpha = 0f)

private fun androidx.compose.ui.draw.CacheDrawScope.clipPathOf(shape: Shape): Path =
    Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@clipPathOf)) }
