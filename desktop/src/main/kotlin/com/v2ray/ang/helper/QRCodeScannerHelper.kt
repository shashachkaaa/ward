package com.v2ray.ang.helper

import androidx.activity.ComponentActivity
import com.v2ray.ang.util.QRCodeDecoder
import com.ward.desktop.FileDialogs

/**
 * «Сканировать QR». Камеры у компьютера обычно нет, а QR-код приходит картинкой:
 * снимком экрана или файлом. Поэтому сперва смотрим картинку в буфере обмена,
 * а если её нет - предлагаем выбрать файл.
 */
class QRCodeScannerHelper(private val activity: ComponentActivity) {
    fun launch(onResult: (String?) -> Unit) {
        val fromClipboard = runCatching {
            val cb = java.awt.Toolkit.getDefaultToolkit().systemClipboard
            if (cb.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.imageFlavor)) {
                val img = cb.getData(java.awt.datatransfer.DataFlavor.imageFlavor) as java.awt.Image
                val buf = java.awt.image.BufferedImage(img.getWidth(null), img.getHeight(null), java.awt.image.BufferedImage.TYPE_INT_ARGB)
                buf.createGraphics().apply { drawImage(img, 0, 0, null); dispose() }
                QRCodeDecoder.syncDecodeQRCode(android.graphics.Bitmap(buf))
            } else null
        }.getOrNull()
        if (fromClipboard != null) {
            onResult(fromClipboard)
            return
        }
        val file = FileDialogs.open("image/*") ?: return onResult(null)
        onResult(QRCodeDecoder.syncDecodeQRCode(file.absolutePath))
    }
}
