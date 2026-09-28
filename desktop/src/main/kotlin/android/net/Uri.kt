package android.net

import java.io.File
import java.net.URI

/** Адрес ресурса. На компьютере это обычно файл, выбранный в диалоге, или ссылка. */
class Uri private constructor(private val raw: String) {
    private val parsed: URI? = runCatching { URI(raw) }.getOrNull()

    val scheme: String? get() = parsed?.scheme
    val host: String? get() = parsed?.host
    val port: Int get() = parsed?.port ?: -1
    val path: String? get() = parsed?.path
    val fragment: String? get() = parsed?.fragment
    val query: String? get() = parsed?.query
    val lastPathSegment: String? get() = path?.substringAfterLast('/')
    val isAbsolute: Boolean get() = parsed?.isAbsolute == true

    fun getQueryParameter(name: String): String? =
        query?.split('&')?.map { it.split('=', limit = 2) }
            ?.firstOrNull { it[0] == name }?.getOrNull(1)
            ?.let { java.net.URLDecoder.decode(it, "UTF-8") }

    /** Файл за адресом, если это file:. */
    fun toFile(): File = File(parsed!!)

    override fun toString(): String = raw
    override fun equals(other: Any?): Boolean = other is Uri && other.raw == raw
    override fun hashCode(): Int = raw.hashCode()

    companion object {
        @JvmStatic fun parse(s: String): Uri = Uri(s)
        @JvmStatic fun fromFile(f: File): Uri = Uri(f.toURI().toString())
        @JvmField val EMPTY = Uri("")
    }
}

fun String.toUri(): Uri = Uri.parse(this)
