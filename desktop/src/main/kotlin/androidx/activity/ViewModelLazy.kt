package androidx.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

/**
 * ViewModel экрана, как `by viewModels()` на Android. Без фабрики ViewModel
 * создаётся конструктором без аргументов или с Application.
 */
inline fun <reified VM : ViewModel> ComponentActivity.viewModels(
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> = lazy {
    val factory = factoryProducer?.invoke() ?: com.ward.desktop.DefaultViewModelFactory
    ViewModelProvider.create(viewModelStore, factory)[VM::class]
}
