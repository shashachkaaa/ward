package androidx.core.content

import android.content.Context
import android.net.Uri
import java.io.File

/** На Android - раздача файла другим приложениям. На компьютере это просто путь к файлу. */
object FileProvider {
    @JvmStatic fun getUriForFile(context: Context, authority: String, file: File): Uri = Uri.fromFile(file)
}
