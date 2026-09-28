package androidx.compose.ui.res

/*
 * Функции ресурсов Compose под Android, которых нет в Compose Desktop. Лежат в
 * том же пакете, что и оригиналы, - поэтому скопированные экраны находят их по
 * своим же импортам и остаются копиями.
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.ward.desktop.AndroidResources
import org.jetbrains.skia.Image
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.xml.sax.InputSource

@Composable
fun stringResource(id: Int): String = remember(id) { AndroidResources.string(id) }

@Composable
fun stringResource(id: Int, vararg formatArgs: Any): String = AndroidResources.string(id, *formatArgs)

@Composable
fun stringArrayResource(id: Int): Array<String> = remember(id) { AndroidResources.array(id).toTypedArray() }

@Composable
fun pluralStringResource(id: Int, count: Int): String = AndroidResources.plural(id, count)

@Composable
fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String =
    AndroidResources.plural(id, count, *formatArgs)

/** Картинка по идентификатору: векторная из drawable/ или растровая из mipmap. */
@Composable
fun painterResource(id: Int): Painter {
    val density = LocalDensity.current
    return when ((id ushr 16) and 0xff) {
        0x04, 0x08 -> rememberVectorPainter(remember(id) { vectorResource(id, density) })
        else -> remember(id) { BitmapPainter(bitmapResource(id)) }
    }
}

@Composable
fun vectorResource(id: Int): ImageVector {
    val density = LocalDensity.current
    return remember(id) { vectorResource(id, density) }
}

private fun vectorResource(id: Int, density: Density): ImageVector {
    val name = AndroidResources.drawableName(id)
    val stream = AndroidResources::class.java.getResourceAsStream("/android-res/drawable/$name.xml")
        ?: error("drawable $name not found")
    return stream.use { loadXmlImageVector(InputSource(it), density) }
}

fun bitmapResource(id: Int): ImageBitmap {
    val name = AndroidResources.mipmapName(id)
    val bytes = AndroidResources::class.java.getResourceAsStream("/android-res/mipmap/$name.png")!!.use { it.readBytes() }
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}
