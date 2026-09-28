package com.v2ray.ang.ui.compose

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Заметки к релизу в диалоге обновления.
 *
 * Текст приходит с GitHub размеченным, и раньше он показывался как есть - со
 * звёздочками вместо жирного. Полноценный markdown сюда тащить незачем: заметки
 * пишем мы сами и пользуемся четырьмя приёмами - заголовок, жирный, курсив и
 * моноширинный.
 *
 * Второе: длинные заметки просто обрезались. Диалог не прокручивается сам, а его
 * содержимое растёт, пока хватает места, и молча упирается в кнопки.
 */
@Composable
fun ReleaseNotesText(notes: String, modifier: Modifier = Modifier) {
    val annotated = remember(notes) { parseSimpleMarkdown(notes) }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            // Выше этого диалог начал бы упираться в края экрана
            .heightIn(max = 360.dp)
            .verticalScroll(rememberScrollState())
    )
}

/** Заголовок раздела: крупнее и жирнее обычного текста, но без своего кегля в теме. */
private val HeadingStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)

/**
 * Длина метки заголовка в этом месте, включая пробел за решётками, или 0.
 *
 * Заголовком считается только начало строки: от одной до шести решёток и пробел -
 * ровно как в markdown. Решётка посреди строки разметкой не является.
 */
private fun headingAt(text: String, index: Int): Int {
    if (index > 0 && text[index - 1] != '\n') return 0
    var hashes = 0
    while (index + hashes < text.length && text[index + hashes] == '#' && hashes < 6) hashes++
    if (hashes == 0) return 0
    val after = index + hashes
    if (after >= text.length || text[after] != ' ') return 0
    return hashes + 1
}

/** Границы разметки: чем окружён кусок и как его показывать. */
private val markers = listOf(
    "**" to SpanStyle(fontWeight = FontWeight.Bold),
    "`" to SpanStyle(fontFamily = FontFamily.Monospace),
    "*" to SpanStyle(fontStyle = FontStyle.Italic)
)

/**
 * Разбирает жирный, курсив и моноширинный текст.
 *
 * Двойные звёздочки проверяются раньше одинарных: иначе `**жирный**` разобрался бы
 * как курсив с лишними звёздочками по краям.
 *
 * Вложенность не поддерживается - внутри выделения текст берётся как есть. Заметки
 * к релизу её не используют, а поддержка стоила бы разбора в дерево.
 */
fun parseSimpleMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        // Заголовок разбирается первым и только с начала строки: решётка посреди
        // текста - это решётка, а не разметка. На странице релиза заголовки делят
        // заметки на разделы, и без разбора они приезжали сюда решётками в текст
        val headingLength = headingAt(text, i)
        if (headingLength > 0) {
            val start = i + headingLength
            val end = text.indexOf('\n', start).takeIf { it >= 0 } ?: text.length
            pushStyle(HeadingStyle)
            append(text.substring(start, end))
            pop()
            i = end
            continue
        }

        var matched = false
        for ((token, style) in markers) {
            if (!text.startsWith(token, i)) continue
            val start = i + token.length
            // За открывающей меткой пробел не идёт - так markdown и отличает выделение
            // от маркера списка. Без этого строка «* пункт с **жирным**» разбиралась
            // в кашу: звёздочка списка считалась открывающим курсивом
            if (start < text.length && text[start].isWhitespace()) continue
            val end = text.indexOf(token, start)
            // Пустую пару и незакрытую разметку оставляем обычным текстом. Без этой
            // проверки незакрытый «**» съедался как пустой курсив: одна звёздочка
            // считалась открывающей, вторая - тут же закрывающей
            if (end <= start) continue
            pushStyle(style)
            append(text.substring(start, end))
            pop()
            i = end + token.length
            matched = true
            break
        }
        if (!matched) {
            append(text[i])
            i++
        }
    }
}
