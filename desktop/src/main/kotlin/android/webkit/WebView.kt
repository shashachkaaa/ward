package android.webkit

import android.content.Context
import android.view.View

/** Веб-страница. Встроенного браузера нет - страницу показывает AndroidView текстом. */
class WebView(context: Context) : View(context) {
    var url: String? = null
        private set

    fun loadUrl(url: String) { this.url = url }
}
