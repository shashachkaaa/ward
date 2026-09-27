package android.content

import com.ward.desktop.DesktopPaths
import java.io.File

/**
 * Контекст приложения. На настольной системе он один на всё приложение, и от
 * него нужно только одно - где лежат файлы.
 */
open class Context {
    val filesDir: File get() = DesktopPaths.dataDir

    companion object {
        /** Единственный экземпляр: его и передают туда, где скопированный код ждёт контекст. */
        val app = Context()
    }
}
