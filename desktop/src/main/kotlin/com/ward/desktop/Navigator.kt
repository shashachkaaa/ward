package com.ward.desktop

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Стопка экранов в одном окне - то, чем на Android заведует система.
 *
 * Открыть экран значит создать Activity по классу из Intent, вызвать её
 * onCreate и положить сверху; окно рисует верхний. finish() снимает экран и
 * отдаёт результат тому, кто его ждал.
 */
object Navigator {

    val stack: SnapshotStateList<ComponentActivity> = mutableStateListOf()
    private val resultCallbacks = HashMap<ComponentActivity, (ActivityResult) -> Unit>()

    /** Выход из приложения, когда закрыт последний экран. */
    var onEmpty: () -> Unit = {}

    fun start(intent: Intent) {
        val cls = intent.component
        if (cls == null) {
            handleExternal(intent)
            return
        }
        val activity = cls.getDeclaredConstructor().newInstance() as ComponentActivity
        activity.intent = intent
        stack.lastOrNull()?.performPause()
        stack.add(activity)
        activity.performCreate()
        activity.performResume()
    }

    fun startForResult(intent: Intent, onResult: (ActivityResult) -> Unit) {
        if (intent.component == null) {
            handleExternal(intent)
            onResult(ActivityResult(Activity.RESULT_CANCELED, null))
            return
        }
        start(intent)
        stack.lastOrNull()?.let { resultCallbacks[it] = onResult }
    }

    fun finish(activity: Activity) {
        val a = activity as? ComponentActivity ?: return
        val wasTop = stack.lastOrNull() === a
        stack.remove(a)
        a.performDestroy()
        resultCallbacks.remove(a)?.invoke(ActivityResult(a.resultCode, a.resultData))
        if (wasTop) stack.lastOrNull()?.performResume()
        if (stack.isEmpty()) onEmpty()
    }

    fun setContent(activity: ComponentActivity, content: @Composable () -> Unit) {
        activity.content = content
    }

    fun back() {
        if (BackHandlers.dispatch()) return
        stack.lastOrNull()?.onBackPressedDispatcher?.onBackPressed()
    }

    /**
     * Намерения к другим приложениям: открыть ссылку, поделиться. На компьютере
     * ссылка открывается в браузере, а «поделиться» кладёт текст в буфер обмена
     * или показывает файл в папке.
     */
    private fun handleExternal(intent: Intent) {
        val target = if (intent.action == Intent.ACTION_CHOOSER) intent else intent
        when (target.action) {
            Intent.ACTION_VIEW -> target.data?.toString()?.let(Platform::openUrl)
            Intent.ACTION_SEND, Intent.ACTION_CHOOSER -> {
                val stream = target.extras.map[Intent.EXTRA_STREAM] as? android.net.Uri
                val text = target.getStringExtra(Intent.EXTRA_TEXT)
                when {
                    stream != null -> runCatching { Platform.revealFile(stream.toFile()) }
                    text != null -> {
                        com.v2ray.ang.util.Utils.setClipboard(android.content.Context.app, text)
                        DesktopUi.toast(AndroidResources.string(com.v2ray.ang.R.string.toast_success))
                    }
                }
            }
            else -> target.data?.toString()?.takeIf { it.startsWith("http") }?.let(Platform::openUrl)
        }
    }
}
