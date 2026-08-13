package com.v2ray.ang.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.GlassBackdrop

/** Пункты нижней капсулы. */
enum class GlassBarItem { HOME, SETTINGS, ADD }

/** Форма стеклянных таблеток на карточках. */
val GlassCapsuleShape = RoundedCornerShape(50)

private val items = listOf(GlassBarItem.HOME, GlassBarItem.SETTINGS, GlassBarItem.ADD)

/**
 * Нижняя капсула жидкого стекла.
 *
 * Внутри - `LiquidBottomTabs` из каталога примеров Kyant0/AndroidLiquidGlass, той же
 * библиотеки, что и переключатели (Apache 2.0, условия в THIRD_PARTY.md). Оттуда же
 * и всё поведение: капля растёт под пальцем, таскается по капсуле и отпускается на
 * ближайшем пункте, а сама капсула на ходу чуть смещается ей вслед.
 *
 * Подпись оставлена прежней, чтобы главный экран и настройки не трогать.
 *
 * @param backdrop Слой с содержимым экрана, записанный тем, кто рисует контент.
 * @param selected Активный пункт.
 * @param onSelect Выбор пункта.
 */
@Composable
fun LiquidGlassBar(
    backdrop: GlassBackdrop,
    selected: GlassBarItem,
    onSelect: (GlassBarItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = items.indexOf(selected).coerceAtLeast(0)

    LiquidBottomTabs(
        selectedTabIndex = { selectedIndex },
        onTabSelected = { index -> onSelect(items[index.coerceIn(items.indices)]) },
        backdrop = backdrop.backdrop,
        tabsCount = items.size,
        modifier = modifier
    ) {
        items.forEachIndexed { index, item ->
            LiquidBottomTab(onClick = { onSelect(item) }) {
                GlassBarIcon(item = item, active = index == selectedIndex)
            }
        }
    }
}

@Composable
private fun GlassBarIcon(item: GlassBarItem, active: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val tint by animateColorAsState(
        targetValue = if (active) scheme.primary else scheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "glassBarTint"
    )

    Box(contentAlignment = Alignment.Center) {
        when (item) {
            GlassBarItem.HOME -> HomeIcon(color = tint, modifier = Modifier.size(26.dp))
            GlassBarItem.SETTINGS -> Icon(
                painter = painterResource(R.drawable.ic_settings_24dp),
                contentDescription = stringResource(R.string.main_nav_settings),
                tint = tint,
                modifier = Modifier.size(26.dp)
            )

            GlassBarItem.ADD -> Icon(
                painter = painterResource(R.drawable.ic_add_24dp),
                contentDescription = stringResource(R.string.main_nav_add),
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** Домик в том же проволочном стиле, что и остальные рисованные иконки. */
@Composable
private fun HomeIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 4f, cap = StrokeCap.Round)

        // Крыша
        drawLine(color, Offset(w * 0.1f, h * 0.45f), Offset(w * 0.5f, h * 0.12f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.9f, h * 0.45f), strokeWidth = 4f, cap = StrokeCap.Round)
        // Стены
        drawLine(color, Offset(w * 0.22f, h * 0.42f), Offset(w * 0.22f, h * 0.85f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.78f, h * 0.42f), Offset(w * 0.78f, h * 0.85f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.22f, h * 0.85f), Offset(w * 0.78f, h * 0.85f), strokeWidth = 4f, cap = StrokeCap.Round)
        // Дверь
        drawRect(
            color = color,
            topLeft = Offset(w * 0.42f, h * 0.58f),
            size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.27f),
            style = stroke
        )
    }
}
