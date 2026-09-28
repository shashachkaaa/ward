package com.v2ray.ang

import android.app.Application
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.compose.ThemeManager

/**
 * Приложение. Настольная версия: процесс один, WorkManager и MMKV заводить не
 * нужно - хранилище поднимается само при первом обращении.
 */
class AngApplication : Application() {
    companion object {
        val application: AngApplication by lazy { AngApplication() }

        /** Что на Android делает onCreate приложения. Зовёт Main до первого окна. */
        fun init() {
            SettingsManager.initApp(application)
            ThemeManager.refresh()
        }
    }
}
