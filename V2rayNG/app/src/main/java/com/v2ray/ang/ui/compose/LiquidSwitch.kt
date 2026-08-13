package com.v2ray.ang.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.first
import kotlin.math.abs

/**
 * Размеры и коэффициенты взяты из LiquidToggle библиотеки Kyant0/AndroidLiquidGlass
 * (Apache 2.0) - той самой, на которой сделан референс. Трек 64x28, капля 40x24,
 * ход у неё всего 20dp: капля почти во весь трек, ездить ей особо некуда.
 */
private val TrackWidth = 64.dp
private val TrackHeight = 28.dp
private val ThumbWidth = 40.dp
private val ThumbHeight = 24.dp
private val ThumbPadding = 2.dp
private val DragWidth = 20.dp

/** Во сколько раз капля вырастает, пока её держат. */
private const val PressedScale = 1.5f

/**
 * Запас вокруг трека: раздутая капля выходит за его край, и ей нужно место. Это же
 * поле - холст для линзы, а гнуть фон она может только внутри своего слоя.
 */
private val Overflow = 12.dp

/** Насколько близко к цели капля должна подойти, чтобы её отпустило. */
private const val ArrivalThreshold = 0.025f

/**
 * Переключатель со стеклянной каплей.
 *
 * Устройство подсмотрено в исходниках референса, и оно не такое, каким кажется по
 * записи. Никакой отдельной анимации переброса нет: всё делает **удержание**.
 * Прикосновение раздувает каплю в полтора раза и превращает её из плотной белой в
 * стекло - прозрачное, с преломлением и расхождением цветов по ободку. Пока капля
 * едет, она так и остаётся раздутой, а сжимается и снова белеет, только когда
 * доехала. Программное переключение идёт тем же путём: прижать, довезти, отпустить.
 *
 * Отсюда и то, что на записи выглядело как «раздулась, подержалась, схлопнулась»: это
 * не фазы по таймеру, а состояние нажатия. Раньше я задавал их длительностями, и они
 * разъезжались с ходом капли - оттого и рывки.
 *
 * @param checked Включён ли переключатель.
 * @param onCheckedChange Обработчик нажатия или null, если нажатие обрабатывает строка.
 * @param enabled Доступен ли переключатель.
 * @param checkedTrackColor Цвет трека во включённом состоянии.
 */
@Composable
fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedTrackColor: Color = MaterialTheme.colorScheme.secondary
) {
    val isDark = LocalDarkTheme.current
    val density = LocalDensity.current

    val lens = rememberLiquidLens()
    val trackLayer = rememberGraphicsLayer()
    val lensLayer = rememberGraphicsLayer()

    val dragWidthPx = with(density) { DragWidth.toPx() }

    // Доля хода. Её двигают палец и внешнее состояние, а гонится за ней одна
    // жёсткая пружина без колебаний - как в оригинале
    var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    val value = remember { Animatable(if (checked) 1f else 0f) }

    LaunchedEffect(Unit) {
        snapshotFlow { fraction }.collect { target ->
            value.animateTo(target, spring(dampingRatio = 1f, stiffness = 1000f))
        }
    }

    LaunchedEffect(checked) {
        val target = if (checked) 1f else 0f
        if (target != fraction) fraction = target
    }

    val interactionSource = remember { MutableInteractionSource() }
    val fingerDown by interactionSource.collectIsPressedAsState()
    var dragging by remember { mutableStateOf(false) }

    // Отпускает каплю не палец, а прибытие: пока она не доехала, остаётся раздутой.
    // Это и есть то «плато», которое я раньше отмерял таймером
    var held by remember { mutableStateOf(false) }
    LaunchedEffect(fingerDown, dragging, fraction) {
        if (fingerDown || dragging) {
            held = true
            return@LaunchedEffect
        }
        if (!held) return@LaunchedEffect
        snapshotFlow { abs(value.value - fraction) }.first { it < ArrivalThreshold }
        held = false
    }

    val pressProgress by animateFloatAsState(
        targetValue = if (held && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 1000f),
        label = "switchPress"
    )
    val scaleX by animateFloatAsState(
        targetValue = if (held && enabled) PressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f),
        label = "switchScaleX"
    )
    val scaleY by animateFloatAsState(
        targetValue = if (held && enabled) PressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 250f),
        label = "switchScaleY"
    )

    val trackOff =
        if (isDark) Color(0xFF787880).copy(alpha = 0.36f) else Color(0xFF787878).copy(alpha = 0.2f)
    val disabledAlpha = if (enabled) 1f else 0.38f

    val input = if (onCheckedChange != null && enabled) {
        Modifier
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                enabled = true,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .pointerInput(dragWidthPx) {
                var travelled = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragging = true
                        travelled = 0f
                    },
                    onDragEnd = {
                        // Короткое движение - промахнувшийся тап: жест перехватил
                        // его у нажатия, значит и отработать за него должен он же
                        val far = abs(travelled) > dragWidthPx * 0.2f
                        val result = if (far) fraction >= 0.5f else !checked
                        dragging = false
                        fraction = if (result) 1f else 0f
                        if (result != checked) onCheckedChange(result)
                    },
                    onDragCancel = {
                        dragging = false
                        fraction = if (checked) 1f else 0f
                    }
                ) { change, amount ->
                    change.consume()
                    travelled += amount
                    fraction = (fraction + amount / dragWidthPx).coerceIn(0f, 1f)
                }
            }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .size(TrackWidth + Overflow * 2, TrackHeight + Overflow * 2)
            .then(input)
    ) {
        val over = Overflow.toPx()
        val trackW = TrackWidth.toPx()
        val trackH = TrackHeight.toPx()
        val padding = ThumbPadding.toPx()
        val progress = value.value

        // Скорость растягивает каплю вдоль хода и приплющивает поперёк - ровно та
        // же формула, что в оригинале
        val speed = value.velocity / 50f
        val stretchX = 1f / (1f - (speed * 0.75f).coerceIn(-0.2f, 0.2f))
        val stretchY = 1f - (speed * 0.25f).coerceIn(-0.2f, 0.2f)

        val thumbW = ThumbWidth.toPx() * scaleX * stretchX
        val thumbH = ThumbHeight.toPx() * scaleY * stretchY
        val thumbRadius = minOf(thumbW, thumbH) / 2f

        val trackTop = (size.height - trackH) / 2f
        val cy = size.height / 2f
        val cx = over + padding + ThumbWidth.toPx() / 2f + lerp(0f, dragWidthPx, progress)

        val track = lerp(trackOff, checkedTrackColor, progress)
            .let { it.copy(alpha = it.alpha * disabledAlpha) }

        trackLayer.record {
            drawRoundRect(
                color = track,
                topLeft = Offset(over, trackTop),
                size = Size(trackW, trackH),
                cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
            )
        }

        // Преломление и расхождение цветов появляются только под пальцем: плотная
        // белая капля в покое всё равно ничего не показывает
        val effect = if (pressProgress > 0.01f) {
            lens?.effect(
                layerSize = size,
                center = Offset(cx, cy),
                halfExtent = Size(thumbW / 2f, thumbH / 2f),
                radius = thumbRadius,
                thickness = thumbH * 0.45f,
                refraction = 10.dp.toPx() * pressProgress,
                dispersion = 0.3f * pressProgress,
                highlight = 0.12f * pressProgress
            )
        } else {
            null
        }

        if (effect != null) {
            lensLayer.renderEffect = effect
            lensLayer.record { drawLayer(trackLayer) }
            drawLayer(lensLayer)
        } else {
            drawLayer(trackLayer)
        }

        val topLeft = Offset(cx - thumbW / 2f, cy - thumbH / 2f)
        val thumbSize = Size(thumbW, thumbH)
        val corner = CornerRadius(thumbRadius, thumbRadius)
        val bottom = cy + thumbH / 2f

        // Мягкая тень: она у капли есть всегда, и в покое только она и отделяет её
        // от трека
        repeat(3) { step ->
            val grow = (step + 1) * 1.4.dp.toPx()
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.05f * disabledAlpha),
                topLeft = Offset(topLeft.x - grow, topLeft.y - grow * 0.3f),
                size = Size(thumbW + grow * 2f, thumbH + grow * 2f),
                cornerRadius = CornerRadius(thumbRadius + grow, thumbRadius + grow)
            )
        }

        // Тело капли белеет обратно пропорционально нажатию: прижатая - стекло,
        // отпущенная - плотный белый ползунок
        drawRoundRect(
            color = Color.White,
            topLeft = topLeft,
            size = thumbSize,
            cornerRadius = corner,
            alpha = (1f - pressProgress) * disabledAlpha
        )

        // Блик по ободку разгорается вместе с нажатием - у белой капли ему делать
        // нечего
        if (pressProgress > 0.01f) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        Color.White.copy(alpha = 0.1f)
                    ),
                    startY = topLeft.y,
                    endY = bottom
                ),
                topLeft = topLeft,
                size = thumbSize,
                cornerRadius = corner,
                style = Stroke(width = 1.2.dp.toPx()),
                alpha = pressProgress * disabledAlpha
            )
        }
    }
}
