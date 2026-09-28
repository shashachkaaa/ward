package com.v2ray.ang.handler

import android.content.Context
import android.content.Intent
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import com.ward.desktop.DesktopPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/** Состояние установки обновления - те же ступени, что на Android. */
sealed class UpdateInstallState {
    data object Idle : UpdateInstallState()
    data class Downloading(val percent: Int) : UpdateInstallState()
    data object Installing : UpdateInstallState()
    data class Failed(val message: String?) : UpdateInstallState()
    data object NeedsPermission : UpdateInstallState()
}

/**
 * Установка обновления, настольная версия.
 *
 * Установщик (.msi или .deb) скачивается во временную папку и открывается
 * системой: на Windows его ставит msiexec, на Linux - менеджер пакетов. Само
 * приложение при этом закрывается, иначе установщик не сможет заменить файлы.
 */
object AppUpdateInstaller {

    private val _state = MutableStateFlow<UpdateInstallState>(UpdateInstallState.Idle)
    val state: StateFlow<UpdateInstallState> = _state.asStateFlow()

    /** Разрешения ставить приложения на компьютере не спрашивают. */
    fun canInstall(context: Context): Boolean = true

    fun permissionIntent(context: Context): Intent = Intent()

    fun reset() {
        _state.value = UpdateInstallState.Idle
    }

    fun onInstallFinished(error: String?) {
        _state.value = if (error == null) UpdateInstallState.Idle else UpdateInstallState.Failed(error)
    }

    suspend fun downloadAndInstall(context: Context, url: String): Boolean = withContext(Dispatchers.IO) {
        _state.value = UpdateInstallState.Downloading(0)
        val name = url.substringAfterLast('/').ifBlank { if (DesktopPaths.isWindows) "Ward.msi" else "ward.deb" }
        val target = File(context.cacheDir, name)
        val ok = runCatching { download(url, target) }
            .onFailure { LogUtil.e(AppConfig.TAG, "Update download failed", it) }
            .getOrDefault(false)
        if (!ok) {
            _state.value = UpdateInstallState.Failed(null)
            return@withContext false
        }
        _state.value = UpdateInstallState.Installing
        runCatching {
            val cmd = if (DesktopPaths.isWindows) listOf("msiexec", "/i", target.absolutePath)
            else listOf("xdg-open", target.absolutePath)
            ProcessBuilder(cmd).start()
        }.onFailure {
            _state.value = UpdateInstallState.Failed(it.message)
            return@withContext false
        }
        // Установщику нужно заменить файлы приложения - освобождаем их
        if (DesktopPaths.isWindows) {
            Thread { Thread.sleep(1500); com.ward.desktop.AppExit.quit() }.start()
        }
        true
    }

    private fun download(url: String, target: File): Boolean {
        val client = OkHttpClient()
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) return false
            val body = resp.body
            val total = body.contentLength()
            target.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) _state.value = UpdateInstallState.Downloading((done * 100 / total).toInt())
                    }
                }
            }
        }
        return true
    }
}
