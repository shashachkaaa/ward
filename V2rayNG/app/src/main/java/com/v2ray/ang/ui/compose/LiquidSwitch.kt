package com.v2ray.ang.ui.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.catalog.components.LiquidToggle

/**
 * Переключатель со стеклянной каплей.
 *
 * Своей реализации здесь больше нет: сам переключатель - это `LiquidToggle` из
 * каталога примеров библиотеки Kyant0/AndroidLiquidGlass, той самой, на которой
 * сделан референс (Apache 2.0, условия в THIRD_PARTY.md). Три захода написать то же
 * самое вручную кончились ничем: сходство упиралось не в подбор коэффициентов, а в
 * устройство эффекта, и повторять его по видеозаписи оказалось безнадёжно.
 *
 * Фон отдаём пустой - так же, как в референсе. Капле незачем видеть экран: она
 * преломляет собственный трек, и весь эффект складывается внутри самого
 * переключателя.
 *
 * Обёртка нужна ради подписи: у `LiquidToggle` состояние приходит лямбдой, а места
 * вызова у нас держат его значением, и у них есть `enabled`.
 *
 * @param checked Включён ли переключатель.
 * @param onCheckedChange Обработчик переключения или null, если оно недоступно.
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
    val backdrop = remember { emptyBackdrop() }
    val active = enabled && onCheckedChange != null

    LiquidToggle(
        selected = { checked },
        onSelect = { onCheckedChange?.invoke(it) },
        backdrop = backdrop,
        modifier = modifier.alpha(if (active) 1f else 0.38f),
        accentColor = checkedTrackColor,
        enabled = active
    )
}
