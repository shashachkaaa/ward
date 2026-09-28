package com.kyant.backdrop.catalog.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.v2ray.ang.ui.compose.innerEdgeShade
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/** Блик оригинала: складывается с фоном (BlendMode.Plus). */
private val DefaultButtonHighlight: () -> Highlight? = { Highlight.Default }

/** Тень оригинала. */
private val DefaultButtonShadow: () -> Shadow? = { Shadow.Default }

/** Размытие, преломление и оживление цвета - то, ради чего берётся фон. */
private val BackdropEffects: BackdropEffectScope.() -> Unit = {
    vibrancy()
    blur(2f.dp.toPx())
    lens(12f.dp.toPx(), 24f.dp.toPx())
}

/** Тот же набор, выключенный: с пустым фоном ему нечего обрабатывать. */
private val NoBackdropEffects: BackdropEffectScope.() -> Unit = {}

@Composable
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    // Насколько кнопка прибавляет под пальцем и ездит за ним. Разведено, потому что
    // это разные вещи: прибавка идёт кнопке любой формы, а слежение придумано для
    // мелких таблеток - их приятно подталкивать. Полоса во всю ширину от слежения
    // просто выезжает за карточку
    pressGrowth: Dp = 4f.dp,
    followsTouch: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    // Высота и поля у оригинала зашиты под крупную кнопку. Референс добавил себе
    // ровно эти два параметра, чтобы тем же компонентом делать и мелкие чипы
    applyDefaultHeight: Boolean = true,
    contentPaddingHorizontal: Dp = 16f.dp,
    highlight: (() -> Highlight?)? = DefaultButtonHighlight,
    applyEffects: Boolean = true,
    // Толщина стекла. Без фона под кнопкой преломлять нечего, и объём ей приходится
    // давать светом: заливка градиентом, тень внутрь и тень наружу
    surfaceBrush: Brush? = null,
    innerShadow: (() -> InnerShadow?)? = null,
    // Тёмная кромка внутри по верхнему краю - вместо внутренней тени библиотеки.
    // Та держит свой слой видеоядра и запомненный радиус размытия, а в списке слой
    // при переиспользовании строки создаётся заново, радиус же остаётся прежним -
    // и размытие новому слою не назначается. Мягкая кромка после прокрутки вниз и
    // обратно превращалась в резкую черту. Здесь слоя нет, портиться нечему
    innerEdge: Color? = null,
    innerEdgeDepth: Dp = 8f.dp,
    shadow: (() -> Shadow?)? = DefaultButtonShadow,
    content: @Composable RowScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope
        )
    }

    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = if (applyEffects) BackdropEffects else NoBackdropEffects,
                highlight = highlight,
                innerShadow = innerShadow,
                shadow = shadow,
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height

                        val progress = interactiveHighlight.pressProgress

                        // Прирост считается по каждой стороне отдельно. В оригинале
                        // множитель брался от высоты и применялся к обеим сторонам: у
                        // таблетки 30 на 40 точек это давало те же четыре точки и по
                        // ширине, а у кнопки во всю ширину экрана - уже под сорок, и
                        // она вылезала за карточку. Постоянная тут - длина, а не доля,
                        // значит и расти кнопка должна на четыре точки в любую сторону,
                        // какой бы она ни была
                        val growth = pressGrowth.toPx()
                        val growX = growth / width
                        val growY = growth / height

                        val offset = if (followsTouch) interactiveHighlight.offset else Offset.Zero
                        if (followsTouch) {
                            val maxOffset = size.minDimension
                            val initialDerivative = 0.05f
                            translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                            translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)
                        }

                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            lerp(1f, 1f + growX, progress) +
                                    growX * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            lerp(1f, 1f + growY, progress) +
                                    growY * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                    if (surfaceBrush != null) {
                        drawRect(surfaceBrush)
                    }
                }
            )
            .then(
                if (innerEdge != null) {
                    Modifier.innerEdgeShade(innerEdge, Capsule(), innerEdgeDepth)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .then(if (applyDefaultHeight) Modifier.height(48f.dp) else Modifier)
            .padding(horizontal = contentPaddingHorizontal),
        horizontalArrangement = Arrangement.spacedBy(8f.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
