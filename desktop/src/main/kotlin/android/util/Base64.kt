package android.util

/**
 * Base64 с флагами как у Android поверх java.util.Base64.
 *
 * Декодер Android прощает переводы строк, пробелы и отсутствие дополнения, а
 * подписки этим пользуются сплошь и рядом - поэтому перед разбором их убираем
 * и дополнение восстанавливаем сами.
 */
object Base64 {
    const val DEFAULT = 0
    const val NO_PADDING = 1
    const val NO_WRAP = 2
    const val CRLF = 4
    const val URL_SAFE = 8
    const val NO_CLOSE = 16

    @JvmStatic
    fun decode(str: String?, flags: Int): ByteArray {
        var s = (str ?: "").filterNot { it.isWhitespace() }.trimEnd('=')
        val urlSafe = flags and URL_SAFE != 0 || s.contains('-') || s.contains('_')
        if (s.length % 4 == 1) throw IllegalArgumentException("bad base-64")
        s += "=".repeat((4 - s.length % 4) % 4)
        return if (urlSafe) java.util.Base64.getUrlDecoder().decode(s) else java.util.Base64.getDecoder().decode(s)
    }

    @JvmStatic
    fun decode(input: ByteArray, flags: Int): ByteArray = decode(String(input, Charsets.ISO_8859_1), flags)

    @JvmStatic
    fun encode(input: ByteArray, flags: Int): ByteArray = encodeToString(input, flags).toByteArray(Charsets.ISO_8859_1)

    @JvmStatic
    fun encodeToString(input: ByteArray, flags: Int): String {
        var encoder = if (flags and URL_SAFE != 0) java.util.Base64.getUrlEncoder() else java.util.Base64.getEncoder()
        if (flags and NO_PADDING != 0) encoder = encoder.withoutPadding()
        val out = encoder.encodeToString(input)
        if (flags and NO_WRAP != 0) return out
        // Как у Android: строки по 76 символов и перевод строки в конце
        val sep = if (flags and CRLF != 0) "\r\n" else "\n"
        return out.chunked(76).joinToString(sep, postfix = sep)
    }
}
