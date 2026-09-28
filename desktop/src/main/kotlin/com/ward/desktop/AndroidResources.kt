package com.ward.desktop

import com.v2ray.ang.R
import org.w3c.dom.Element
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Строки, массивы и множественные формы из res/ приложения под Android.
 *
 * Идентификаторы из сгенерированного [R] ведут к именам, имена - к значениям.
 * Русские значения берутся для русской системы, для остальных - английские,
 * а чего нет в переводе, берётся из основного файла, как на Android.
 */
object AndroidResources {

    private val russian = Locale.getDefault().language == "ru"

    private class Table(
        val strings: Map<String, String>,
        val arrays: Map<String, List<String>>,
        val plurals: Map<String, Map<String, String>>
    )

    private val base: Table by lazy { load("values") }
    private val localized: Table? by lazy { if (russian) load("values-ru") else null }

    private fun load(dir: String): Table {
        val strings = HashMap<String, String>()
        val arrays = HashMap<String, List<String>>()
        val plurals = HashMap<String, Map<String, String>>()
        for (file in listOf("strings.xml", "arrays.xml")) {
            val stream = javaClass.getResourceAsStream("/android-res/$dir/$file") ?: continue
            val doc = stream.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
            val nodes = doc.documentElement.childNodes
            for (i in 0 until nodes.length) {
                val e = nodes.item(i) as? Element ?: continue
                val name = e.getAttribute("name")
                when (e.tagName) {
                    "string" -> strings[name] = unescape(e.textContent)
                    "string-array", "array", "integer-array" -> arrays[name] = items(e).map { unescape(it.textContent) }
                    "plurals" -> plurals[name] = items(e).associate { it.getAttribute("quantity") to unescape(it.textContent) }
                }
            }
        }
        return Table(strings, arrays, plurals)
    }

    private fun items(e: Element): List<Element> {
        val out = mutableListOf<Element>()
        val nodes = e.childNodes
        for (i in 0 until nodes.length) (nodes.item(i) as? Element)?.takeIf { it.tagName == "item" }?.let(out::add)
        return out
    }

    /**
     * Строка ресурса по правилам Android: пробелы схлопываются, кроме как внутри
     * двойных кавычек, сами кавычки убираются, обратная косая экранирует.
     */
    internal fun unescape(raw: String): String {
        val sb = StringBuilder()
        var quoted = false
        var i = 0
        var lastSpace = false
        val s = raw.trim()
        while (i < s.length) {
            val c = s[i]
            when {
                c == '\\' && i + 1 < s.length -> {
                    when (val n = s[i + 1]) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'u' -> if (i + 5 < s.length) {
                            sb.append(s.substring(i + 2, i + 6).toInt(16).toChar()); i += 4
                        }
                        else -> sb.append(n)
                    }
                    i += 2; lastSpace = false; continue
                }
                c == '"' -> quoted = !quoted
                c.isWhitespace() && !quoted -> {
                    if (!lastSpace) sb.append(' ')
                    lastSpace = true; i++; continue
                }
                else -> sb.append(c)
            }
            lastSpace = false
            i++
        }
        return sb.toString()
    }

    private fun resolve(value: String): String =
        if (value.startsWith("@string/")) stringByName(value.removePrefix("@string/")) else value

    private fun stringByName(name: String): String =
        resolve(localized?.strings?.get(name) ?: base.strings[name] ?: name)

    private fun nameOf(table: Array<String>, id: Int): String =
        table.getOrNull(id and 0xffff) ?: error("unknown resource id 0x${id.toString(16)}")

    fun string(id: Int): String = systemString(id) ?: stringByName(nameOf(R.stringNames, id))

    // Системные строки Android - android.R.string
    private fun systemString(id: Int): String? = when (id) {
        android.R.string.ok -> if (russian) "ОК" else "OK"
        android.R.string.cancel -> if (russian) "Отмена" else "Cancel"
        else -> null
    }

    fun string(id: Int, vararg args: Any?): String = string(id).format(*args)

    fun array(id: Int): List<String> {
        val name = nameOf(R.arrayNames, id)
        return (localized?.arrays?.get(name) ?: base.arrays[name] ?: emptyList()).map(::resolve)
    }

    fun plural(id: Int, count: Int, vararg args: Any?): String {
        val name = nameOf(R.pluralsNames, id)
        val forms = localized?.plurals?.get(name) ?: base.plurals[name] ?: return name
        val key = if (russian && localized?.plurals?.containsKey(name) == true) russianQuantity(count) else englishQuantity(count)
        val template = forms[key] ?: forms["other"] ?: forms.values.first()
        return template.format(*args)
    }

    private fun englishQuantity(n: Int) = if (n == 1) "one" else "other"

    // Правила CLDR для русского: 1, 21, 31 - one; 2-4, 22-24 - few; остальное - many
    private fun russianQuantity(n: Int): String {
        val m10 = n % 10
        val m100 = n % 100
        return when {
            m10 == 1 && m100 != 11 -> "one"
            m10 in 2..4 && m100 !in 12..14 -> "few"
            else -> "many"
        }
    }

    fun drawableName(id: Int): String = when (id) {
        android.R.drawable.ic_menu_close_clear_cancel -> "android_ic_menu_close_clear_cancel"
        android.R.drawable.ic_menu_help, android.R.drawable.sym_def_app_icon -> "ic_about_24dp"
        else -> nameOf(R.drawableNames, id)
    }
    fun mipmapName(id: Int): String = nameOf(R.mipmapNames, id)
}
