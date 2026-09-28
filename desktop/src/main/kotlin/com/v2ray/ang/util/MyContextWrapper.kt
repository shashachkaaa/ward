package com.v2ray.ang.util

import android.content.Context
import java.util.Locale

/** Настольная версия: язык берётся из системы, подменять контекст не нужно. */
object MyContextWrapper {
    fun wrap(context: Context, newLocale: Locale?): Context = context
}
