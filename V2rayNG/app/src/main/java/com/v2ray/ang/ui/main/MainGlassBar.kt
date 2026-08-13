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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay

/** Пункты нижней капсулы. */
enum class GlassBarItem { HOME, SETTINGS, ADD }

/** Форма стеклянных таблеток на карточках. */
val GlassCapsuleShape = RoundedCornerShape(50)

private val items = listOf(GlassBarItem.HOME, GlassBarItem.SETTINGS, GlassBarItem.ADD)

/** Жёсткость хода капли по панели. Мягче зашитой в библиотеке: ход тут длинный. */
private const val TabTravelStiffness = 320f

/** Сколько капля ждёт, прежде чем вернуться с пункта-действия. */
private const val SettleBackDelayMs = 550L

/**
 * Пункты, которые не уводят на свой экран, а делают действие на месте. Капля на них
 * не остаётся: постояла и вернулась. Для остальных она обязана остаться там, куда её
 * послали, - иначе получается тот самый рывок назад с последующим повторным переездом.
 */
private val transientItems = setOf(GlassBarItem.ADD)

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
    val external = items.indexOf(selected).coerceAtLeast(0)

    // Индекс, который видит капля. Ведём его сами и меняем сразу по нажатию: у нас
    // «шестерёнка» и «+» - действия, а не вкладки, и активный пункт после них
    // остаётся прежним. Ждали бы его - капля стояла бы на месте
    var shown by remember { mutableIntStateOf(external) }
    LaunchedEffect(external) { shown = external }

    // «+» открывает шторку, а активным остаётся прежний пункт - капля постоит на
    // плюсе и вернётся. Пунктам, которые уводят на свой экран, возвращаться нельзя:
    // экран как раз открывается, и капля дёргалась бы назад у него на глазах
    LaunchedEffect(shown, external) {
        if (shown != external && items[shown] in transientItems) {
            delay(SettleBackDelayMs)
            if (shown != external) shown = external
        }
    }

    // Лямбду держим одну на всё время: компонент запоминает по ней своё состояние,
    // и новый экземпляр на каждой перекомпоновке сбрасывал бы его
    val shownState = rememberUpdatedState(shown)
    val selectedTabIndex = remember { { shownState.value } }
    val selectedState = rememberUpdatedState(selected)

    LiquidBottomTabs(
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { index ->
            val item = items[index.coerceIn(items.indices)]
            shown = index
            // Сюда же компонент сообщает и о смене состояния снаружи. Отвечать на
            // неё вызовом onSelect нельзя: экран уже там, и мы отправляли бы его
            // туда повторно. Из-за этого возврат из настроек открывал их заново
            if (item != selectedState.value) onSelect(item)
        },
        backdrop = backdrop.backdrop,
        tabsCount = items.size,
        modifier = modifier,
        // Иконку под каплей компонент подкрашивает сам, и по умолчанию - своим
        // синим. Нам нужен акцент приложения
        accentColor = MaterialTheme.colorScheme.primary,
        // Ход у панели во всю ширину, и с зашитой жёсткостью капля перелетала за
        // миг: анимации попросту не было видно
        stiffness = TabTravelStiffness
    ) {
        items.forEachIndexed { index, item ->
            // Нажатие только двигает каплю: о выборе сообщит onTabSelected, когда
            // индекс сменится. Иначе onSelect звался бы дважды
            LiquidBottomTab(onClick = { shown = index }) {
                GlassBarIcon(item = item, active = index == shown)
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
