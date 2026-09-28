package com.v2ray.ang.ui.perappproxy

import android.os.Bundle
import com.v2ray.ang.ui.base.BaseComponentActivity
import androidx.compose.runtime.Composable
import com.ward.desktop.DesktopUi

/**
 * Прокси по приложениям. На компьютере системный прокси общий для всех программ,
 * выбрать часть из них нельзя - экран сразу закрывается с пояснением.
 */
class PerAppProxyActivity : BaseComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DesktopUi.toast(com.ward.desktop.DesktopStrings.notOnDesktop)
        finish()
    }

    @Composable
    override fun ScreenContent() {}
}
