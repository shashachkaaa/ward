package androidx.activity.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext

fun ComponentActivity.setContent(parent: CompositionContext? = null, content: @Composable () -> Unit) {
    com.ward.desktop.Navigator.setContent(this, content)
}

@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    com.ward.desktop.BackHandlerImpl(enabled, onBack)
}
