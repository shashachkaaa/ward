package com.v2ray.ang.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.catalog.components.LiquidSlider
import com.v2ray.ang.R
import kotlin.math.roundToInt

@Composable
fun PreferenceGroupHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 28.dp, top = 22.dp, bottom = 10.dp)
    )
}

/**
 * Скруглённый контейнер, объединяющий несколько строк настроек в один блок.
 */
@Composable
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)),
        content = content
    )
}

/**
 * Row that opens a settings category on its own screen.
 */
@Composable
fun SettingsCategoryItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null
) {
    SettingsItemRow(
        icon = null,
        title = title,
        description = summary,
        enabled = true,
        onClick = onClick,
        modifier = modifier,
        trailing = {
            Icon(
                painter = painterResource(R.drawable.ic_expand_more_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(-90f)
            )
        }
    )
}

/**
 * @param clickExcludesTrailing Не считать нажатием по строке нажатие по тому, что
 *   стоит справа. Нужно строкам с переключателем: у него свой обработчик, а строка
 *   вокруг него ловила тот же палец и подсвечивалась целиком - будто нажали её.
 *   Здесь нажатие просто не доходит до строки, вместо того чтобы гасить его задним
 *   числом: перехватывать чужой жест и потом отменять - это гонка, и выигрывает её
 *   то один, то другой.
 */
@Composable
private fun SettingsItemRow(
    icon: Painter?,
    title: String,
    description: String?,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    clickExcludesTrailing: Boolean = false
) {
    val titleColor = if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val descriptionColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

    // Лёгкое проседание строки под пальцем вместо резкой заливки. Заливки здесь
    // нарочно нет: рябь во всю строку - это разметка Android, а не наша, и рядом с
    // жидким стеклом она смотрится чужой
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = tween(120),
        label = "rowScale"
    )

    val clickable =
        if (onClick != null) {
            Modifier.clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
        } else {
            Modifier
        }

    val label: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = titleColor
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            if (!description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp,
                    color = descriptionColor
                )
            }
        }
    }

    if (clickExcludesTrailing && trailing != null) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .scale(scale),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(clickable)
                    .padding(start = 20.dp, end = 8.dp)
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = label
            )
            Box(modifier = Modifier.padding(end = 20.dp)) {
                trailing()
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .scale(scale)
                .then(clickable)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            label()
            trailing?.invoke()
        }
    }
}

/**
 * Строка «название - значение» только для чтения. По нажатию значение можно скопировать:
 * то же HWID руками не перепишешь.
 */
@Composable
fun SettingsInfoItem(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    summary: String? = null,
    onClick: (() -> Unit)? = null
) {
    SettingsItemRow(
        icon = null,
        title = title,
        description = summary,
        enabled = true,
        onClick = onClick,
        modifier = modifier,
        trailing = if (value == null) null else {
            {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .widthIn(max = 190.dp)
                )
            }
        }
    )
}

/**
 * Row for a file: name, timestamp and size, opening the file on click.
 */
@Composable
fun SettingsFileItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null
) {
    SettingsItemRow(
        icon = null,
        title = title,
        description = subtitle,
        enabled = true,
        onClick = onClick,
        modifier = modifier,
        trailing = {
            if (!trailingText.isNullOrEmpty()) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                painter = painterResource(R.drawable.ic_expand_more_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(-90f)
            )
        }
    )
}

@Composable
fun SettingsEditItem(
    icon: Painter? = null,
    title: String,
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    keyboardNumber: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }
    val description = if (isPassword) {
        if (value.isEmpty()) null else "******"
    } else {
        value.ifEmpty { null }
    }

    SettingsItemRow(
        icon = icon,
        title = title,
        description = description,
        enabled = enabled,
        onClick = if (enabled) {
            { showDialog = true }
        } else null,
        modifier = modifier
    )

    if (showDialog) {
        var text by remember { mutableStateOf(value) }
        InputDialog(
            title = title,
            fields = listOf(
                InputField(
                    label = title,
                    value = text,
                    visualTransformation = VisualTransformation.None
                )
            ),
            onFieldChange = { _, v -> text = v },
            confirmText = stringResource(android.R.string.ok),
            dismissText = stringResource(android.R.string.cancel),
            onConfirm = { showDialog = false; onValueChanged(text) },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Числовая настройка ползунком вместо поля ввода.
 *
 * Значение хранится строкой - таким его ждёт ядро, - но пустая строка означает
 * «по умолчанию», и её надо уметь показать: пока пользователь ползунок не трогал,
 * в строке стоит подпись значения по умолчанию, а не число.
 *
 * @param value Текущее значение или пустая строка, если оно не задано.
 * @param defaultValue Что подставить, когда значение не задано.
 * @param valueRange Границы ползунка.
 * @param step Шаг округления.
 * @param valueLabel Как показать число в строке.
 */
@Composable
fun SettingsSliderItem(
    title: String,
    value: String,
    defaultValue: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    enabled: Boolean = true,
    step: Int = 1,
    valueLabel: (Int) -> String = { it.toString() }
) {
    val scheme = MaterialTheme.colorScheme
    val current = value.toIntOrNull() ?: defaultValue

    // Ползунок ведёт непрерывную величину, а наружу уходит округлённая по шагу.
    // Округлять прямо в обработчике нельзя: перетаскивание идёт мелкими приращениями
    // от текущего значения, и пока приращения не набрали полшага, наружу ничего не
    // уходило, текущее значение не менялось - и следующее приращение считалось от
    // того же места. Капля стояла намертво, и работали только нажатия по дорожке
    var position by remember { mutableFloatStateOf(current.toFloat()) }
    // Значение сменилось не отсюда - подхватываем. Своё же округление не трогаем,
    // иначе оно дёргало бы каплю обратно на шаг посреди перетаскивания
    LaunchedEffect(current) {
        if (position.snapTo(step) != current) position = current.toFloat()
    }

    // Ползунку нужен фон, а он тут внутри содержимого экрана: экран сам пишется в
    // слой фона, и рисовать слой внутри его же записи нельзя. Каплю преломляет
    // собственная дорожка ползунка - её слой он заводит себе сам
    val backdrop = remember { emptyBackdrop() }

    Column(modifier = modifier.fillMaxWidth()) {
        SettingsItemRow(
            icon = icon,
            title = title,
            // Число видно всегда, в том числе пока оно не задано: ползунок без
            // подписи не прочитать
            description = valueLabel(current),
            enabled = enabled,
            onClick = null
        )
        LiquidSlider(
            value = { position },
            onValueChange = { raw ->
                if (enabled) {
                    position = raw.coerceIn(valueRange)
                    val snapped = position.snapTo(step)
                    if (snapped != current) onValueChanged(snapped.toString())
                }
            },
            valueRange = valueRange,
            // Порог покоя пружины, а не шаг: с крупным порогом капля замирает, не
            // доехав до места
            visibilityThreshold = (valueRange.endInclusive - valueRange.start) / 1000f,
            backdrop = backdrop,
            accentColor = if (enabled) scheme.primary else scheme.outlineVariant,
            trackColor = scheme.onSurfaceVariant.copy(alpha = 0.2f),
            // Капля матовая, а не глухая: сквозь неё видно размытую дорожку, а под
            // пальцем плёнка сходит совсем и остаётся чистое стекло с преломлением
            thumbColor = Color.White.copy(alpha = if (LocalDarkTheme.current) 0.72f else 0.86f),
            blurs = LocalGlassQuality.current.blurs,
            refracts = LocalGlassQuality.current.refracts,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
        )
    }
}

/** Округление до ближайшего шага. */
private fun Float.snapTo(step: Int): Int = (this / step).roundToInt() * step

@Composable
fun SettingsListItem(
    icon: Painter? = null,
    title: String,
    entries: List<String>,
    values: List<String>,
    selectedValue: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val selectedIndex = values.indexOf(selectedValue).let { if (it >= 0) it else 0 }
    val summary = entries.getOrNull(selectedIndex).orEmpty()

    SettingsItemRow(
        icon = icon,
        title = title,
        description = summary.ifEmpty { null },
        enabled = enabled,
        onClick = if (enabled) {
            { showDialog = true }
        } else null,
        modifier = modifier
    )

    if (showDialog) {
        SelectListDialog(
            title = title,
            options = entries,
            selectedOption = summary,
            onSelected = { index, _ ->
                showDialog = false
                values.getOrNull(index)?.let(onSelected)
            },
            onDismiss = { showDialog = false },
            showRadio = true
        )
    }
}

@Composable
fun SettingsMenuItem(
    icon: Painter? = null,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    SettingsItemRow(
        icon = icon,
        title = title,
        description = subtitle,
        enabled = enabled,
        onClick = if (enabled) onClick else null,
        modifier = modifier
    )
}

@Composable
fun SettingsSwitchItem(
    icon: Painter? = null,
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    SettingsItemRow(
        icon = icon,
        title = title,
        description = summary,
        enabled = enabled,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else null,
        modifier = modifier,
        // Нажатие по самому переключателю - его дело, а не строки
        clickExcludesTrailing = true,
        trailing = {
            LiquidSwitch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled
            )
        }
    )
}
