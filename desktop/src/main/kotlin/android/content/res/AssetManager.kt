package android.content.res

import com.ward.desktop.DesktopPaths
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * Ассеты приложения. На Android они в APK; здесь их два источника: ресурсы
 * сборки (шаблоны конфига, правила маршрутизации) и файлы, положенные рядом с
 * установленным приложением (ядро и geo-файлы).
 */
class AssetManager internal constructor() {
    fun list(path: String): Array<String> {
        val bundled = File(DesktopPaths.bundledDir, path).list().orEmpty().toList()
        return bundled.distinct().toTypedArray()
    }

    fun open(fileName: String): InputStream {
        File(DesktopPaths.bundledDir, fileName).takeIf { it.isFile }?.let { return it.inputStream() }
        return AssetManager::class.java.getResourceAsStream("/assets/$fileName")
            ?: throw FileNotFoundException(fileName)
    }

    companion object {
        internal val instance = AssetManager()
    }
}
