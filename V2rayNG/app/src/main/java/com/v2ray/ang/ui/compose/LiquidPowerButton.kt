package com.v2ray.ang.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.ui.main.PowerIcon
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

private val ButtonSize = 168.dp
private val GlowSize = 240.dp

/**
 * Кнопка подключения из настоящего жидкого стекла.
 *
 * Ключевая мысль: линзе нужно что преломлять, а под кнопкой лежит ровный фон экрана,
 * и передать ей общий слой нельзя - содержимое экрана само в него пишется. Поэтому
 * кнопка носит фон с собой: под стеклом лежит своё пятно акцента со своим слоем, и
 * стекло гнёт именно его. Состояние читается по тому, как это пятно разгорается, а
 * при подключении вокруг ещё бежит дуга.
 *
 * Стекло, блик, тени и преломление - из Kyant0/AndroidLiquidGlass (Apache 2.0,
 * условия в THIRD_PARTY.md).
 *
 * @param isConnected Туннель поднят.
 * @param isConnecting Идёт подключение или отключение.
 * @param statusText Подпись состояния.
 * @param timeString Время сеанса, показывается только на подключении.
 */
@Composable
fun LiquidPowerButton(
    isConnected: Boolean,
    isConnecting: Boolean,
    statusText: String,
    timeString: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val gravityAngle = rememberGravityAngle()

    // Одна величина на всё: 0 - покой, 1 - соединение установлено
    val active by animateFloatAsState(
        targetValue = when {
            isConnected -> 1f
            isConnecting -> 0.55f
            else -> 0f
        },
        animationSpec = tween(500),
        label = "powerActive"
    )
    val accent by animateColorAsState(
        targetValue = if (isConnected || isConnecting) scheme.primary else scheme.outlineVariant,
        animationSpec = tween(500),
        label = "powerAccent"
    )

    val transition = rememberInfiniteTransition(label = "power")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "powerSweep"
    )

    val pressed = remember { MutableInteractionSource() }
    val isPressed by pressed.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(180),
        label = "powerPress"
    )

    // Пятно под стеклом. Ему нужен свой слой: именно его линза и будет гнуть
    val glow = rememberLayerBackdrop()

    Box(modifier = modifier.size(GlowSize), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(GlowSize)
                .layerBackdrop(glow)
                .drawBehind {
                    // Мягкое пятно акцента - тем ярче, чем ближе к подключению
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.10f + 0.45f * active),
                                accent.copy(alpha = 0.04f + 0.18f * active),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension / 2f
                        )
                    )
                    // Бегущая дуга: пока идёт подключение - крутится, на связи -
                    // замирает ровным кольцом
                    val ring = ButtonSize.toPx() / 2f + 10.dp.toPx()
                    val stroke = 3.dp.toPx()
                    rotate(if (isConnected) 0f else sweep) {
                        drawArc(
                            color = accent.copy(alpha = 0.25f + 0.55f * active),
                            startAngle = -90f,
                            sweepAngle = if (isConnected) 360f else 110f,
                            useCenter = false,
                            topLeft = Offset(center.x - ring, center.y - ring),
                            size = Size(ring * 2f, ring * 2f),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }
        )

        Box(
            modifier = Modifier
                .size(ButtonSize)
                .drawBackdrop(
                    backdrop = glow,
                    shape = { CircleShape },
                    effects = {
                        vibrancy()
                        blur(6f.dp.toPx())
                        // Преломление на всю стенку: кнопка крупная, и именно на
                        // ней стекло видно лучше всего
                        lens(24f.dp.toPx(), 48f.dp.toPx(), chromaticAberration = true)
                    },
                    // Блик едет за наклоном телефона - настоящее стекло ловит свет
                    // под тем углом, под которым его держат. Угол читается здесь, а
                    // не снаружи: так датчик трогает только отрисовку, а не всю
                    // перекомпоновку кнопки.
                    // Блендинг оставлен обычным: складывающийся (по умолчанию у этого
                    // стиля - Plus) зависит от того, как собирается кадр, а кадр под
                    // кнопкой пересобирается постоянно - и бегущей дугой, и таймером
                    highlight = {
                        Highlight(
                            style = HighlightStyle.Default(
                                angle = gravityAngle.value,
                                falloff = 2f,
                                blendMode = DrawScope.DefaultBlendMode
                            )
                        )
                    },
                    shadow = {
                        Shadow(radius = 24f.dp, color = Color.Black.copy(alpha = 0.10f + 0.10f * active))
                    },
                    innerShadow = { InnerShadow(radius = 12f.dp * press, alpha = press) },
                    layerBlock = {
                        // Под пальцем кнопка слегка проседает
                        val scale = 1f - 0.04f * press
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = {
                        drawCircle(scheme.surface.copy(alpha = 0.30f - 0.10f * active))
                    }
                )
                .background(Color.Transparent, CircleShape)
                .clickable(
                    interactionSource = pressed,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PowerIcon(
                    color = if (isConnected || isConnecting) accent else scheme.onSurfaceVariant,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = statusText,
                    color = scheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                AnimatedVisibility(
                    visible = isConnected,
                    enter = fadeIn(tween(400)) + expandVertically(tween(400)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeString,
                            color = scheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
