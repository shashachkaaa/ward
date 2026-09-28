package com.ward.desktop

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.reflect.KClass

/** Связь кода без Compose с окном: короткие сообщения и просьбы вывести окно вперёд. */
object DesktopUi {
    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    fun toast(text: String) {
        _toasts.tryEmit(text)
    }
}

/**
 * Фабрика ViewModel по умолчанию, как у ComponentActivity на Android: конструктор
 * с Application, а если его нет - без аргументов.
 */
object DefaultViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        val cls = modelClass.java
        cls.constructors.firstOrNull { c -> c.parameterTypes.size == 1 && Application::class.java.isAssignableFrom(c.parameterTypes[0]) }
            ?.let { @Suppress("UNCHECKED_CAST") return it.newInstance(Application.instance) as T }
        @Suppress("UNCHECKED_CAST")
        return cls.getDeclaredConstructor().newInstance() as T
    }
}

/** Обработчики «назад» из экранов: последний включённый перехватывает Esc и стрелку. */
internal object BackHandlers {
    val handlers = mutableListOf<() -> Boolean>()

    fun dispatch(): Boolean = handlers.asReversed().any { it() }
}

@Composable
internal fun BackHandlerImpl(enabled: Boolean, onBack: () -> Unit) {
    val current = rememberUpdatedState(onBack)
    val on = rememberUpdatedState(enabled)
    DisposableEffect(Unit) {
        val h: () -> Boolean = { if (on.value) { current.value(); true } else false }
        BackHandlers.handlers += h
        onDispose { BackHandlers.handlers -= h }
    }
}
