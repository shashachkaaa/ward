package com.ward.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.compose.DesktopTheme
import com.v2ray.ang.ui.compose.LocalServiceColors
import com.ward.desktop.core.AppController
import com.ward.desktop.core.ConnectionState
import com.ward.desktop.ui.MainScreen
import com.ward.desktop.ui.SettingsDialog
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/**
 * Второй экземпляр запускать нельзя: оба полезут на один порт и в один системный
 * прокси, и выключение одного оставит второй без интернета.
 */
private fun acquireSingleInstance(): FileLock? = try {
    RandomAccessFile(File(DesktopPaths.dataDir, "ward.lock"), "rw").channel.tryLock()
} catch (_: Exception) {
    null
}

fun main() {
    val lock = acquireSingleInstance()
    if (lock == null) {
        JOptionPane.showMessageDialog(null, "Ward уже запущен - он в области уведомлений.", "Ward", JOptionPane.INFORMATION_MESSAGE)
        exitProcess(0)
    }

    val controller = AppController()
    // Выход любым путём - закрытие, выключение компьютера, Ctrl+C в консоли -
    // не должен оставлять системный прокси, указывающий в никуда
    Runtime.getRuntime().addShutdownHook(Thread { controller.shutdown() })

    application {
        var visible by remember { mutableStateOf(true) }
        var settings by remember { mutableStateOf(false) }
        val state by controller.state.collectAsState()
        val icon = painterResource("ward.png")

        fun quit() {
            controller.shutdown()
            exitApplication()
        }

        // Области уведомлений бывает и нет - в части окружений Linux. Тогда прятать
        // окно некуда: вернуть его будет нечем, и закрытие означает выход
        val hasTray = isTraySupported
        if (hasTray) Tray(
            icon = icon,
            tooltip = if (state is ConnectionState.Connected) "Ward - подключён" else "Ward",
            onAction = { visible = true },
            menu = {
                Item("Открыть", onClick = { visible = true })
                Item(
                    if (state is ConnectionState.Disconnected) "Подключить" else "Отключить",
                    onClick = controller::toggle
                )
                Separator()
                Item("Выход", onClick = ::quit)
            }
        )

        // Закрытие окна прячет его в трей, а не завершает работу: подключение
        // должно жить, пока человек не выйдет явно
        Window(
            onCloseRequest = { if (hasTray) visible = false else quit() },
            visible = visible,
            title = "Ward",
            icon = icon,
            state = rememberWindowState(size = DpSize(480.dp, 860.dp))
        ) {
            window.minimumSize = java.awt.Dimension(400, 600)
            DesktopTheme {
                CompositionLocalProvider(
                    LocalServiceColors provides MmkvManager.decodeSettingsBool(AppConfig.PREF_SERVICE_COLORS, true)
                ) {
                    MainScreen(controller, onOpenSettings = { settings = true })
                    if (settings) SettingsDialog(onDismiss = { settings = false })
                }
            }
        }
    }
}
