package android.graphics

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

/** Растровая картинка поверх BufferedImage - столько, сколько нужно QR-кодам и значкам. */
class Bitmap(val image: BufferedImage) {
    val width: Int get() = image.width
    val height: Int get() = image.height
    var isRecycled = false
        private set

    fun getPixel(x: Int, y: Int): Int = image.getRGB(x, y)
    fun setPixel(x: Int, y: Int, color: Int) = image.setRGB(x, y, color)
    fun getPixels(pixels: IntArray, offset: Int, stride: Int, x: Int, y: Int, width: Int, height: Int) {
        image.getRGB(x, y, width, height, pixels, offset, stride)
    }
    fun setPixels(pixels: IntArray, offset: Int, stride: Int, x: Int, y: Int, width: Int, height: Int) {
        image.setRGB(x, y, width, height, pixels, offset, stride)
    }
    fun recycle() { isRecycled = true }

    fun compress(format: CompressFormat, quality: Int, stream: java.io.OutputStream): Boolean =
        ImageIO.write(image, if (format == CompressFormat.PNG) "png" else "jpg", stream)

    enum class Config { ARGB_8888, RGB_565, ALPHA_8 }
    enum class CompressFormat { PNG, JPEG, WEBP }

    companion object {
        @JvmStatic
        fun createBitmap(width: Int, height: Int, config: Config = Config.ARGB_8888): Bitmap =
            Bitmap(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB))

        @JvmStatic
        fun createBitmap(pixels: IntArray, width: Int, height: Int, config: Config): Bitmap =
            createBitmap(width, height, config).apply { setPixels(pixels, 0, width, 0, 0, width, height) }

        @JvmStatic
        fun createScaledBitmap(src: Bitmap, w: Int, h: Int, filter: Boolean): Bitmap {
            val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            out.createGraphics().apply { drawImage(src.image, 0, 0, w, h, null); dispose() }
            return Bitmap(out)
        }
    }
}

object BitmapFactory {
    class Options {
        @JvmField var inJustDecodeBounds = false
        @JvmField var inSampleSize = 1
        @JvmField var outWidth = 0
        @JvmField var outHeight = 0
        @JvmField var inPreferredConfig: Bitmap.Config = Bitmap.Config.ARGB_8888
    }

    @JvmStatic
    fun decodeByteArray(data: ByteArray, offset: Int, length: Int, opts: Options? = null): Bitmap? =
        decode(runCatching { ImageIO.read(ByteArrayInputStream(data, offset, length)) }.getOrNull(), opts)

    @JvmStatic
    fun decodeFile(path: String, opts: Options? = null): Bitmap? =
        decode(runCatching { ImageIO.read(File(path)) }.getOrNull(), opts)

    @JvmStatic
    fun decodeStream(stream: java.io.InputStream?, outPadding: Any? = null, opts: Options? = null): Bitmap? =
        decode(stream?.let { runCatching { ImageIO.read(it) }.getOrNull() }, opts)

    private fun decode(img: BufferedImage?, opts: Options?): Bitmap? {
        img ?: return null
        opts?.outWidth = img.width
        opts?.outHeight = img.height
        if (opts?.inJustDecodeBounds == true) return null
        // ImageIO отдаёт картинку в родном формате; приводим к ARGB, как Android
        val argb = if (img.type == BufferedImage.TYPE_INT_ARGB) img else
            BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB).also {
                it.createGraphics().apply { drawImage(img, 0, 0, null); dispose() }
            }
        val sample = (opts?.inSampleSize ?: 1).coerceAtLeast(1)
        val bmp = Bitmap(argb)
        return if (sample == 1) bmp else Bitmap.createScaledBitmap(bmp, img.width / sample, img.height / sample, true)
    }
}

/** Холст поверх картинки - на нём рисуют значки. */
class Canvas(val bitmap: Bitmap) {
    val width: Int get() = bitmap.width
    val height: Int get() = bitmap.height
}
