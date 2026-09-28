package android.content

import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

/** Доступ к данным по адресу. На компьютере адреса - это файлы из диалогов. */
class ContentResolver internal constructor() {
    fun openInputStream(uri: Uri): InputStream? = runCatching { uri.toFile().inputStream() }.getOrNull()
    fun openOutputStream(uri: Uri): OutputStream? = runCatching { uri.toFile().outputStream() }.getOrNull()
    fun openOutputStream(uri: Uri, mode: String): OutputStream? =
        runCatching { java.io.FileOutputStream(uri.toFile(), mode.contains('a')) }.getOrNull()
    fun getType(uri: Uri): String? = null

    fun query(uri: Uri, projection: Array<String>?, selection: String?, args: Array<String>?, sort: String?): android.database.Cursor? =
        runCatching {
            val f = uri.toFile()
            android.database.Cursor(mapOf("_display_name" to f.name, "_size" to f.length()))
        }.getOrNull()

    companion object {
        internal val instance = ContentResolver()
    }
}

/** Данные для буфера обмена или «поделиться». */
class ClipData private constructor(val label: CharSequence?, val uri: Uri?, val text: CharSequence?) {
    companion object {
        @JvmStatic fun newPlainText(label: CharSequence?, text: CharSequence?) = ClipData(label, null, text)
        @JvmStatic fun newUri(resolver: ContentResolver?, label: CharSequence?, uri: Uri) = ClipData(label, uri, null)
        @JvmStatic fun newRawUri(label: CharSequence?, uri: Uri) = ClipData(label, uri, null)
    }
}
