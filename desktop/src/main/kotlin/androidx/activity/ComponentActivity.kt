package androidx.activity

import android.app.Activity
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Экран с Compose-содержимым, жизненным циклом и своими ViewModel. Содержимое
 * рисует окно приложения - см. [com.ward.desktop.Navigator].
 */
open class ComponentActivity : Activity(), LifecycleOwner, ViewModelStoreOwner {

    private val registry = LifecycleRegistry.createUnsafe(this)
    override val lifecycle: Lifecycle get() = registry
    override val viewModelStore: ViewModelStore = ViewModelStore()

    internal var content: (@Composable () -> Unit)? by mutableStateOf(null)

    val onBackPressedDispatcher = OnBackPressedDispatcher { finish() }

    internal fun moveTo(state: Lifecycle.State) {
        if (registry.currentState != Lifecycle.State.DESTROYED) registry.currentState = state
    }

    internal fun performCreate() {
        onCreate(null)
        moveTo(Lifecycle.State.CREATED)
    }

    internal fun performResume() {
        onStart(); moveTo(Lifecycle.State.STARTED)
        onResume(); moveTo(Lifecycle.State.RESUMED)
    }

    internal fun performPause() {
        onPause(); moveTo(Lifecycle.State.STARTED)
    }

    internal fun performDestroy() {
        onStop(); moveTo(Lifecycle.State.CREATED)
        onDestroy()
        registry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }

    override fun onCreate(savedInstanceState: Bundle?) {}

    fun <I, O> registerForActivityResult(
        contract: androidx.activity.result.contract.ActivityResultContract<I, O>,
        callback: androidx.activity.result.ActivityResultCallback<O>
    ): androidx.activity.result.ActivityResultLauncher<I> =
        object : androidx.activity.result.ActivityResultLauncher<I>() {
            override fun launch(input: I) =
                contract.launch(this@ComponentActivity, input) { callback.onActivityResult(it) }
        }

    open fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean = false
}

/** Назад - по кнопке «назад» в шапке экрана или по Esc. */
class OnBackPressedDispatcher(private val fallback: () -> Unit) {
    private val callbacks = mutableListOf<OnBackPressedCallback>()

    fun addCallback(owner: LifecycleOwner, callback: OnBackPressedCallback) { callbacks += callback }
    fun addCallback(callback: OnBackPressedCallback) { callbacks += callback }

    fun onBackPressed() {
        callbacks.lastOrNull { it.isEnabled }?.handleOnBackPressed() ?: fallback()
    }
}

abstract class OnBackPressedCallback(var isEnabled: Boolean) {
    abstract fun handleOnBackPressed()
    fun remove() {}
}

fun ComponentActivity.enableEdgeToEdge() {}
