package com.v2ray.ang.ui.apppicker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.ward.desktop.DesktopUi

/**
 * Выбор приложений для правила маршрутизации. На компьютере списка приложений
 * Android нет - экран закрывается, выбор остаётся прежним.
 */
class AppPickerActivity : BaseComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DesktopUi.toast(com.ward.desktop.DesktopStrings.notOnDesktop)
        finish()
    }

    @Composable
    override fun ScreenContent() {}

    companion object {
        private const val EXTRA_SELECTED_PACKAGES = "selected_packages"
        private const val EXTRA_PICKER_TITLE = "picker_title"

        fun createIntent(context: Context, selectedPackages: Collection<String> = emptyList(), title: String? = null): Intent =
            Intent(context, AppPickerActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_SELECTED_PACKAGES, ArrayList(selectedPackages))
                title?.let { putExtra(EXTRA_PICKER_TITLE, it) }
            }

        fun getSelectedPackages(intent: Intent?): List<String> =
            intent?.getStringArrayListExtra(EXTRA_SELECTED_PACKAGES).orEmpty()
    }
}
