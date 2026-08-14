package com.v2ray.ang.ui.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.highlight.Highlight

/**
 * Стеклянная кнопка: чипы под кнопкой подключения, таблетки на карточках подписок.
 *
 * Фон отдаём пустой, и это не упрощение, а единственный возможный путь. Такие кнопки
 * живут внутри содержимого экрана, а оно само пишется в слой фона; рисовать слой
 * внутри его же записи нельзя. Референс поступает так же: своим кнопкам он тоже
 * передаёт пустой фон, а стеклом их делают блик, тень и отклик на палец - у
 * `LiquidButton` кнопка тянется за пальцем и слегка вспухает под нажатием.
 *
 * @param tint Сплошная заливка акцентом. У `LiquidButton` она кроет на 75% и гасит
 *   собственный цвет содержимого - годится кнопке с белой иконкой, но не чипу с
 *   акцентным текстом. По умолчанию её нет.
 * @param surfaceColor Заливка поверх стекла.
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
    content: @Composable RowScope.() -> Unit
) {
    val backdrop = remember { emptyBackdrop() }
    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier,
        tint = tint,
        surfaceColor = surfaceColor,
        applyDefaultHeight = applyDefaultHeight,
        contentPaddingHorizontal = contentPaddingHorizontal,
        // Блик по умолчанию складывается с тем, что под ним (BlendMode.Plus), а
        // результат сложения зависит от того, как собирается кадр. Внутри списка кадр
        // на прокрутке пересобирается каждый раз, и блик с него пропадал - кнопка
        // теряла стекло, пока список едет. Ambient кладётся поверх обычным способом,
        // на нём же сделано и всё остальное стекло приложения
        highlight = AmbientHighlight,
        // Фон пустой - размывать и преломлять нечего. Это не экономия: лишний слой с
        // эффектом переписывается каждый кадр и ровно ничего не даёт
        applyEffects = false,
        content = content
    )
}

/** Блик, который не зависит от способа сборки кадра. */
private val AmbientHighlight: () -> Highlight? = { Highlight.Ambient }
