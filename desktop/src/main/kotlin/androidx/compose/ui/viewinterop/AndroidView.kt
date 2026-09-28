package androidx.compose.ui.viewinterop

import android.view.View
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

/**
 * Вид Android внутри Compose. Настольная версия умеет один вид - WebView со
 * страницей из ассетов: страница показывается текстом, без разметки.
 */
@Composable
fun <T : View> AndroidView(factory: (android.content.Context) -> T, modifier: Modifier = Modifier, update: (T) -> Unit = {}) {
    val context = LocalContext.current
    val view = remember { factory(context).also(update) }
    val text = remember(view) {
        val url = (view as? WebView)?.url.orEmpty()
        val html = if (url.startsWith("file:///android_asset/")) {
            View::class.java.getResourceAsStream("/assets/" + url.removePrefix("file:///android_asset/"))
                ?.use { it.readBytes().decodeToString() }.orEmpty()
        } else url
        html.replace(Regex("(?is)<(script|style).*?</\\1>"), "")
            .replace(Regex("(?i)<br\\s*/?>|</p>|</li>|</h\\d>|</tr>"), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
            .lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }
    Box(modifier.verticalScroll(rememberScrollState())) { Text(text, fontSize = 12.sp) }
}
