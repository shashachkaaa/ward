package com.v2ray.ang.handler

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Значок сервиса из заголовка подписки `profile-icon`.
 *
 * Значение приходит либо картинкой прямо в заголовке (`base64:...`), либо ссылкой.
 * Первое лучше: картинка приезжает вместе с подпиской, и никуда больше приложение
 * не ходит. Ссылка же означает запрос с устройства пользователя на чужой хост -
 * тот узнает адрес человека и что у него стоит Ward. Для клиента, которым
 * пользуются ради обратного, это стоит знать; поэтому ссылки разрешены только по
 * https и ходим по ним редко - при обновлении подписки, а дальше из кэша.
 *
 * Картинка приходит от постороннего, поэтому у неё есть потолок: и по весу, и по
 * размеру в точках. Без этого достаточно прислать PNG 20000x20000, чтобы уложить
 * приложение по памяти - файл при этом весит килобайты.
 */
object SubscriptionIconLoader {

    /** Больше этого не скачиваем: значок в разы меньше, остальное - повод не верить. */
    private const val MAX_BYTES = 256 * 1024L

    /** До скольки точек ужимаем. Плитка на карточке меньше, запас - на плотные экраны. */
    private const val MAX_SIZE_PX = 192

    private const val BASE64_PREFIX = "base64:"

    private val memory = ConcurrentHashMap<String, ImageBitmap>()

    /** Значок, если он уже разобран. Дёшево и без сети - для отрисовки. */
    fun cached(source: String): ImageBitmap? = memory[source]

    /**
     * Разбирает значок: из base64 сразу, ссылку - из кэша на диске или из сети.
     *
     * @return Картинка или null, если значка нет либо он не разобрался. Отсутствие
     *   значка не ошибка: карточка нарисует флаг или глобус, как рисовала всегда.
     */
    suspend fun load(context: Context, source: String): ImageBitmap? {
        if (source.isBlank()) return null
        memory[source]?.let { return it }

        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                when {
                    source.startsWith(BASE64_PREFIX) -> decode(
                        Base64.decode(source.substringAfter(BASE64_PREFIX), Base64.DEFAULT)
                    )

                    source.startsWith("https://") -> decode(fetch(context, source))

                    // http и всё прочее - мимо: значок не стоит того, чтобы ради него
                    // ходить открытым текстом
                    else -> {
                        LogUtil.w(AppConfig.TAG, "profile-icon: не https и не base64, пропускаем")
                        null
                    }
                }
            }.onFailure {
                LogUtil.w(AppConfig.TAG, "profile-icon: не разобрался", it)
            }.getOrNull()
        } ?: return null

        memory[source] = bitmap
        return bitmap
    }

    /** Скачивает по ссылке, держа файл в кэше: второй раз в сеть уже не идём. */
    private fun fetch(context: Context, url: String): ByteArray? {
        val dir = File(context.cacheDir, "sub-icons").apply { mkdirs() }
        val file = File(dir, digest(url))

        if (!file.exists() || file.length() == 0L) {
            // Сначала напрямую, потом через локальный прокси - тем же путём, что и
            // сама подписка: если до хоста нет дороги, обычно она есть через туннель
            val ok = download(url, file, httpPort = 0) ||
                    download(url, file, httpPort = SettingsManager.getHttpPort())
            if (!ok) {
                runCatching { file.delete() }
                return null
            }
        }

        if (file.length() > MAX_BYTES) {
            LogUtil.w(AppConfig.TAG, "profile-icon: ${file.length()} байт - слишком много")
            runCatching { file.delete() }
            return null
        }
        return file.readBytes()
    }

    private fun download(url: String, target: File, httpPort: Int): Boolean =
        HttpUtil.downloadToFile(
            request = UrlContentRequest(url = url, timeout = 15000, httpPort = httpPort),
            targetFile = target
        )

    /**
     * Раскладывает картинку, сперва узнав её размер и не разворачивая целиком.
     * Огромную ужимаем на лету - разложенная в память она весит четыре байта на точку.
     */
    private fun decode(bytes: ByteArray?): ImageBitmap? {
        if (bytes == null || bytes.isEmpty()) return null
        if (bytes.size > MAX_BYTES) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > MAX_SIZE_PX || bounds.outHeight / sample > MAX_SIZE_PX) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    }

    private fun digest(url: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)
}
