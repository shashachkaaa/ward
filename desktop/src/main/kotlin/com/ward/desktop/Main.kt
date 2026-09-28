package com.ward.desktop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.bitmapResource
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.Notification
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.handler.CrashReportManager
import com.v2ray.ang.handler.SubscriptionUpdater
import com.v2ray.ang.ui.compose.AppSnackbarManager
import com.v2ray.ang.ui.main.MainActivity
import com.ward.desktop.core.SystemProxy
import libv2ray.CoreProcesses
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

/** Выход из приложения любым путём: ядро и системный прокси не должны нас пережить. */
object AppExit {
    @Volatile var onQuit: () -> Unit = { exitProcess(0) }

    fun shutdown() {
        runCatching { CoreServiceManager.stopBlocking() }
        runCatching { CoreProcesses.stopAll() }
        SystemProxy.disable()
    }

    fun quit() {
        shutdown()
        onQuit()
    }
}

fun main() {
    if (acquireSingleInstance() == null) {
        JOptionPane.showMessageDialog(null, "Ward уже запущен - он в области уведомлений.", "Ward", JOptionPane.INFORMATION_MESSAGE)
        exitProcess(0)
    }

    val app = AngApplication.application
    CrashReportManager.install(app)
    // После аварийного выхода: вернуть системный прокси и добить оставшееся ядро
    SystemProxy.recoverIfDirty()
    CoreProcesses.killStale()
    AngApplication.init()
    SubscriptionUpdater.sync(app)
    Runtime.getRuntime().addShutdownHook(Thread { AppExit.shutdown() })

    application {
        var visible by remember { mutableStateOf(true) }
        var running by remember { mutableStateOf(CoreServiceManager.isRunning()) }
        val icon = remember { BitmapPainter(bitmapResource(R.mipmap.ic_launcher)) }
        val trayState = rememberTrayState()
        val hasTray = isTraySupported

        // Состояние подключения для трея - из тех же сообщений, что получает экран
        LaunchedEffect(Unit) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.getIntExtra("key", 0)) {
                        AppConfig.MSG_STATE_START_SUCCESS, AppConfig.MSG_STATE_RUNNING -> running = true
                        AppConfig.MSG_STATE_STOP_SUCCESS, AppConfig.MSG_STATE_NOT_RUNNING,
                        AppConfig.MSG_STATE_START_FAILURE -> running = false
                    }
                }
            }
            app.registerReceiver(receiver, IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY))
            DesktopNotifications.sender = { title, text -> trayState.sendNotification(Notification(title, text)) }
            WindowControl.onHide = { if (hasTray) visible = false }
            WindowControl.onShow = { visible = true }
            AppExit.onQuit = ::exitApplication
            Navigator.onEmpty = { if (hasTray) visible = false else AppExit.quit() }
            if (Navigator.stack.isEmpty()) Navigator.start(Intent(app, MainActivity::class.java))
        }

        // Области уведомлений бывает и нет - в части окружений Linux. Тогда прятать
        // окно некуда: вернуть его будет нечем, и закрытие означает выход
        if (hasTray) Tray(
            icon = icon,
            state = trayState,
            tooltip = if (running) "Ward - " + DesktopStrings.connected else "Ward",
            onAction = { visible = true },
            menu = {
                Item(DesktopStrings.open, onClick = { visible = true })
                Item(
                    if (running) DesktopStrings.disconnect else DesktopStrings.connect,
                    onClick = {
                        if (running) LauncherManager.stopService(app) else LauncherManager.startServiceFromToggle(app)
                    }
                )
                Separator()
                Item(DesktopStrings.exit, onClick = AppExit::quit)
            }
        )

        Window(
            onCloseRequest = { if (hasTray) visible = false else AppExit.quit() },
            visible = visible,
            title = "Ward",
            icon = icon,
            state = rememberWindowState(size = DpSize(460.dp, 900.dp)),
            onPreviewKeyEvent = { e ->
                // Esc - как системная «назад» на Android
                if (e.key == Key.Escape && e.type == KeyEventType.KeyDown && Navigator.stack.size > 1) {
                    Navigator.back(); true
                } else false
            }
        ) {
            window.minimumSize = java.awt.Dimension(380, 600)
            FileDialogs.owner = window
            val top = Navigator.stack.lastOrNull()
            if (top != null) {
                CompositionLocalProvider(LocalContext provides top) {
                    top.content?.invoke()
                }
            }
            // Сообщения из кода без Compose - плашкой приложения
            LaunchedEffect(Unit) {
                DesktopUi.toasts.collect { AppSnackbarManager.show(it) }
            }
        }
    }
}
