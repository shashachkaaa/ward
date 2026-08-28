package com.v2ray.ang.ui.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * Стеклянная кнопка: чипы под кнопкой подключения, таблетки на карточках подписок.
 *
 * Фон отдаём пустой, и это не упрощение, а единственный возможный путь. Такие кнопки
 * живут внутри содержимого экрана, а оно само пишется в слой фона; рисовать слой
 * внутри его же записи нельзя. Референс поступает так же: своим кнопкам он тоже
 * передаёт пустой фон.
 *
 * Отсюда и главная сложность: преломлять нечего, а под кнопкой всё равно ровный фон
 * экрана - даже будь слой, гнуть в нём было бы нечего. Значит объём приходится давать
 * светом, как это делает и стекло поверх сплошной поверхности в жизни:
 *
 * - заливка не ровная, а градиентом: сверху стекло светлее, снизу гуще;
 * - тень внутрь по нижней кромке - это и есть видимая толщина стенки;
 * - тень наружу - кнопка приподнята над фоном, а не нарисована на нём;
 * - блик направленный и едет за наклоном телефона - тот же, что у кнопки подключения,
 *   так что весь экран освещён с одной стороны.
 *
 * Без всего этого оставались ровная заливка и тонкий контур - кнопка читалась
 * нарисованной, а не стеклянной.
 *
 * @param tint Сплошная заливка акцентом. У `LiquidButton` она кроет на 75% и гасит
 *   собственный цвет содержимого - годится кнопке с белой иконкой, но не чипу с
 *   акцентным текстом. По умолчанию её нет.
 * @param surfaceColor Заливка поверх стекла. Из неё же строится градиент.
 * @param applyDefaultHeight Держать высоту 48dp. Чипам она велика.
 * @param contentPaddingHorizontal Поля содержимого.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    applyDefaultHeight: Boolean = true,
    contentPaddingHorizontal: Dp = 16.dp,
    isInteractive: Boolean = true,
    pressGrowth: Dp = 4.dp,
    followsTouch: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val backdrop = remember { emptyBackdrop() }
    val isDark = LocalDarkTheme.current
    val gravityAngle = rememberGravityAngle()

    // Плёнка света поверх заливки: сверху подсветка, снизу затемнение. Величины
    // небольшие намеренно - заметный перепад читается как кнопка с градиентом, а не
    // как стекло. Кнопке совсем без заливки плёнка ни к чему: подсвечивать нечего
    val surfaceBrush = remember(surfaceColor, isDark) {
        if (!surfaceColor.isSpecified) {
            null
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (isDark) 0.10f else 0.22f),
                    Color.Transparent,
                    Color.Black.copy(alpha = if (isDark) 0.10f else 0.05f)
                )
            )
        }
    }

    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier,
        tint = tint,
        surfaceColor = surfaceColor,
        applyDefaultHeight = applyDefaultHeight,
        contentPaddingHorizontal = contentPaddingHorizontal,
        isInteractive = isInteractive,
        pressGrowth = pressGrowth,
        followsTouch = followsTouch,
        // Блик по наклону, как у кнопки подключения. Блендинг обычный: складывающийся
        // (он у этого стиля по умолчанию) зависит от того, как собирается кадр, а
        // внутри списка кадр на прокрутке пересобирается каждый раз - блик с него
        // пропадал, и кнопка теряла стекло, пока список едет
        highlight = {
            Highlight(
                width = 1f.dp,
                style = HighlightStyle.Default(
                    color = Color.White.copy(alpha = if (isDark) 0.6f else 0.9f),
                    angle = gravityAngle.value,
                    falloff = 2f,
                    blendMode = DrawScope.DefaultBlendMode
                )
            )
        },
        // Толщина стенки: тёмная кромка внутри, с той стороны, куда свет не достаёт
        innerShadow = {
            InnerShadow(
                radius = 6f.dp,
                offset = DpOffset(0f.dp, 2f.dp),
                color = Color.Black.copy(alpha = if (isDark) 0.28f else 0.16f)
            )
        },
        // Кнопка приподнята над фоном. Тень оригинала под мелкий чип слишком широкая
        shadow = {
            Shadow(
                radius = 8f.dp,
                offset = DpOffset(0f.dp, 2f.dp),
                color = Color.Black.copy(alpha = if (isDark) 0.35f else 0.14f)
            )
        },
        surfaceBrush = surfaceBrush,
        // Фон пустой - размывать и преломлять нечего. Это не экономия: лишний слой с
        // эффектом переписывается каждый кадр и ровно ничего не даёт
        applyEffects = false,
        content = content
    )
}
