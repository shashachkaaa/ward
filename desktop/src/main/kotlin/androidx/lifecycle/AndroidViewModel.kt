package androidx.lifecycle

import android.app.Application

/** ViewModel с доступом к приложению, как на Android. */
open class AndroidViewModel(private val application: Application) : ViewModel() {
    @Suppress("UNCHECKED_CAST")
    open fun <T : Application> getApplication(): T = application as T
}
