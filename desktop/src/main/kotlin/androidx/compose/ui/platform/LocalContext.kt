package androidx.compose.ui.platform

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

/** Контекст для экранов - как на Android. Окно подставляет сюда текущий экран. */
val LocalContext = staticCompositionLocalOf<Context> { Context.app }
