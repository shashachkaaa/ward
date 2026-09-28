package android.content

import com.ward.desktop.AndroidResources
import com.ward.desktop.DesktopPaths
import com.ward.desktop.Navigator
import java.io.File

/**
 * Контекст приложения. На компьютере приложение одно и процесс один: контекст
 * знает, где лежат файлы, откуда брать строки и как открыть другой экран.
 */
open class Context {
    open val filesDir: File get() = DesktopPaths.dataDir
    open val cacheDir: File get() = File(DesktopPaths.dataDir, "cache").apply { mkdirs() }
    open val applicationContext: Context get() = app
    open val packageName: String get() = "com.ward.client"

    open val packageManager: android.content.pm.PackageManager get() = android.content.pm.PackageManager.instance

    open val assets: android.content.res.AssetManager get() = android.content.res.AssetManager.instance

    open val contentResolver: ContentResolver get() = ContentResolver.instance

    open val resources: android.content.res.Resources get() = android.content.res.Resources.instance

    fun getString(resId: Int): String = AndroidResources.string(resId)
    fun getString(resId: Int, vararg formatArgs: Any?): String = AndroidResources.string(resId, *formatArgs)
    fun getText(resId: Int): CharSequence = getString(resId)

    fun getDir(name: String, mode: Int): File = File(DesktopPaths.dataDir, name).apply { mkdirs() }
    fun getExternalFilesDir(type: String?): File? =
        (if (type == null) DesktopPaths.dataDir else File(DesktopPaths.dataDir, type)).apply { mkdirs() }

    open fun startActivity(intent: Intent) = Navigator.start(intent)

    // Широковещательные сообщения - внутри процесса: интерфейс и «службы» живут
    // вместе, и сообщение просто доходит до всех, кто подписан на его действие
    fun sendBroadcast(intent: Intent) = Broadcasts.send(this, intent)
    fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter): Intent? {
        Broadcasts.register(receiver, filter); return null
    }
    fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter, flags: Int): Intent? =
        registerReceiver(receiver, filter)
    fun unregisterReceiver(receiver: BroadcastReceiver) = Broadcasts.unregister(receiver)

    companion object {
        /** Контекст приложения: его и передают туда, где скопированный код ждёт контекст. */
        val app: Context by lazy { android.app.Application.instance }

        const val MODE_PRIVATE = 0
    }
}
