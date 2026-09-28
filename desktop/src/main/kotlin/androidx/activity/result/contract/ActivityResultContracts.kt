package androidx.activity.result.contract

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import com.ward.desktop.FileDialogs
import com.ward.desktop.Navigator

/**
 * Договор «запустить и получить ответ». На Android ответ приходит от другого
 * приложения; здесь - от экрана в той же стопке или от системного диалога файлов.
 */
abstract class ActivityResultContract<I, O> {
    abstract fun launch(context: Context, input: I, onResult: (O) -> Unit)
}

object ActivityResultContracts {

    class StartActivityForResult : ActivityResultContract<Intent, ActivityResult>() {
        override fun launch(context: Context, input: Intent, onResult: (ActivityResult) -> Unit) {
            Navigator.startForResult(input, onResult)
        }
    }

    class GetContent : ActivityResultContract<String, Uri?>() {
        override fun launch(context: Context, input: String, onResult: (Uri?) -> Unit) =
            onResult(FileDialogs.open(input)?.let(Uri::fromFile))
    }

    class OpenDocument : ActivityResultContract<Array<String>, Uri?>() {
        override fun launch(context: Context, input: Array<String>, onResult: (Uri?) -> Unit) =
            onResult(FileDialogs.open(input.firstOrNull() ?: "*/*")?.let(Uri::fromFile))
    }

    class CreateDocument(private val mimeType: String = "*/*") : ActivityResultContract<String, Uri?>() {
        override fun launch(context: Context, input: String, onResult: (Uri?) -> Unit) =
            onResult(FileDialogs.save(input)?.let(Uri::fromFile))
    }

    class RequestPermission : ActivityResultContract<String, Boolean>() {
        override fun launch(context: Context, input: String, onResult: (Boolean) -> Unit) = onResult(true)
    }
}
