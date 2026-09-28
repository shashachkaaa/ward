package android.app

import android.content.Context
import android.content.Intent
import com.ward.desktop.Navigator

/**
 * Экран. На компьютере все экраны живут в одном окне стопкой: открыть - положить
 * сверху, закрыть - снять.
 */
open class Activity : Context() {
    // В Activity на Java есть и свойство intent, и методы getIntent/setIntent - код
    // с Android пользуется обоими. У свойства в Kotlin те же имена на уровне JVM,
    // поэтому его аксессорам даны другие
    @get:JvmName("intentValue") @set:JvmName("intentValue")
    lateinit var intent: Intent
    internal var resultCode: Int = RESULT_CANCELED
    internal var resultData: Intent? = null
    var isFinishing: Boolean = false
        private set

    override val applicationContext: Context get() = Application.instance
    val application: Application get() = Application.instance

    /** «Свернуть» - на компьютере это спрятать окно в трей. */
    fun moveTaskToBack(nonRoot: Boolean): Boolean {
        com.ward.desktop.WindowControl.hide(); return true
    }

    // Анимации переходов между экранами на компьютере не нужны
    fun overridePendingTransition(enterAnim: Int, exitAnim: Int) {}
    fun overrideActivityTransition(type: Int, enterAnim: Int, exitAnim: Int) {}

    fun setIntent(newIntent: Intent) { intent = newIntent }
    fun getIntent(): Intent = intent

    open fun finish() {
        if (isFinishing) return
        isFinishing = true
        Navigator.finish(this)
    }

    fun setResult(code: Int) { resultCode = code }
    fun setResult(code: Int, data: Intent?) { resultCode = code; resultData = data }

    /** На Android сюда приходит базовый контекст; на компьютере он один и тот же. */
    protected open fun attachBaseContext(newBase: Context?) {}

    open fun onCreate(savedInstanceState: android.os.Bundle?) {}
    open fun onStart() {}
    open fun onResume() {}
    open fun onPause() {}
    open fun onStop() {}
    open fun onDestroy() {}
    open fun onNewIntent(intent: Intent) { this.intent = intent }

    companion object {
        const val RESULT_OK = -1
        const val RESULT_CANCELED = 0
        const val OVERRIDE_TRANSITION_OPEN = 0
        const val OVERRIDE_TRANSITION_CLOSE = 1
    }
}
