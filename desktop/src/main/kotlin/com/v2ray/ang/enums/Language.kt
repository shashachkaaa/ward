package com.v2ray.ang.enums

/**
 * Языки, на которых говорит приложение.
 *
 * Их два. Остальные достались от v2rayNG и не поддерживались: половина строк там
 * оставалась английской, а приложение при этом обещало человеку его язык. Выбранный
 * когда-то и убранный отсюда код читается как AUTO - язык берётся системный.
 */
enum class Language(val code: String) {
    AUTO("auto"),
    ENGLISH("en"),
    RUSSIAN("ru");

    companion object {
        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: AUTO
        }
    }
}
