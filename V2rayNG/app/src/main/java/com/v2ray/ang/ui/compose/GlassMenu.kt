package com.v2ray.ang.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/** Отступ меню от края экрана: вплотную к нему панель не прижимается. */
private val MenuScreenMargin = 8.dp

/** Насколько меню приподнято в начале появления. */
private val MenuSlideDistance = 10.dp

/**
 * Меню-список поверх экрана: то, что открывается троеточием у подписки, сервера
 * или в шапке раздела.
 *
 * Своё, а не материаловское, ради того как оно появляется. Материал разворачивает
 * меню масштабированием, и наша стеклянная панель едет внутри этого масштаба: каждый
 * кадр она перерисовывается в новом размере, а вместе с размером пересчитывается и
 * то, что она размывает. Размытие с преломлением - самое дорогое, что есть в
 * отрисовке, считать его двенадцать раз за сто миллисекунд никто не успевает, и
 * открытие выходило рваным.
 *
 * Здесь панель появляется в своём окончательном размере. Меняются только прозрачность
 * и небольшой сдвиг - и то и другое видеоядро делает готовой картинкой, ничего не
 * пересчитывая. Смотрится это спокойнее, а стоит почти ничего.
 *
 * @param expanded Открыто ли меню.
 * @param onDismissRequest Просьба закрыть: нажали мимо или назад.
 * @param offset Сдвиг относительно того, от чего меню открывается.
 * @param content Строки меню - обычные DropdownMenuItem.
 */
@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    // Состояние перехода живёт дольше, чем сам признак: пока меню угасает, окно
    // должно оставаться на месте, иначе оно просто исчезнет без анимации
    val transition = remember { MutableTransitionState(false) }
    transition.targetState = expanded

    if (!transition.currentState && !transition.targetState && transition.isIdle) return

    val density = LocalDensity.current
    val margin = with(density) { MenuScreenMargin.roundToPx() }
    val positionProvider = remember(offset, density, margin) {
        GlassMenuPositionProvider(
            contentOffset = with(density) { IntOffset(offset.x.roundToPx(), offset.y.roundToPx()) },
            verticalMargin = margin
        )
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        AnimatedVisibility(
            visibleState = transition,
            enter = fadeIn(tween(durationMillis = 180, easing = LinearOutSlowInEasing)) +
                    slideInVertically(tween(durationMillis = 220, easing = LinearOutSlowInEasing)) {
                        -with(density) { MenuSlideDistance.roundToPx() }
                    },
            // Уходит быстрее, чем появляется: закрытие - уже решённое действие,
            // и ждать его дольше, чем нужно, незачем
            exit = fadeOut(tween(durationMillis = 120))
        ) {
            Column(
                modifier = modifier
                    .glassPanel(GlassMenuShape)
                    // Ширина по самой длинной строке: без этого строки разъезжаются
                    // по своей ширине, и панель выглядит рваной по краю
                    .width(IntrinsicSize.Max)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                content = content
            )
        }
    }
}

/**
 * Куда встать меню относительно того, от чего его открыли.
 *
 * Сначала под ним, не хватило места снизу - над ним, не хватило и там - прижимаем
 * к нижнему краю экрана. По горизонтали так же: сначала по левому краю кнопки,
 * потом по правому, потом в границы окна.
 */
private class GlassMenuPositionProvider(
    private val contentOffset: IntOffset,
    private val verticalMargin: Int
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val startAligned = anchorBounds.left + contentOffset.x
        val endAligned = anchorBounds.right - contentOffset.x - popupContentSize.width
        val candidatesX = if (layoutDirection == LayoutDirection.Ltr) {
            sequenceOf(startAligned, endAligned)
        } else {
            sequenceOf(endAligned, startAligned)
        }
        val x = candidatesX.firstOrNull {
            it >= 0 && it + popupContentSize.width <= windowSize.width
        } ?: (windowSize.width - popupContentSize.width).coerceAtLeast(0)

        val below = anchorBounds.bottom + contentOffset.y
        val above = anchorBounds.top - contentOffset.y - popupContentSize.height
        val y = sequenceOf(below, above).firstOrNull {
            it >= verticalMargin && it + popupContentSize.height <= windowSize.height - verticalMargin
        } ?: (windowSize.height - popupContentSize.height - verticalMargin).coerceAtLeast(verticalMargin)

        return IntOffset(x, y)
    }
}
