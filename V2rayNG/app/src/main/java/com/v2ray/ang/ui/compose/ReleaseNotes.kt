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

/**
 * Заметки к релизу в диалоге обновления.
 *
 * Текст приходит с GitHub размеченным, и раньше он показывался как есть - со
 * звёздочками вместо жирного. Полноценный markdown сюда тащить незачем: заметки
 * пишем мы сами и пользуемся ровно тремя приёмами - жирный, курсив и моноширинный.
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
