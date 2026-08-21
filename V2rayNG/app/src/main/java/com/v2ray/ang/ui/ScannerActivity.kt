package com.v2ray.ang.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.enums.PermissionType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.ui.base.HelperBaseComponentActivity
import com.v2ray.ang.ui.compose.GlassSurface
import com.v2ray.ang.ui.compose.glassBackdropSource
import com.v2ray.ang.ui.compose.rememberGlassBackdrop
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.QRCodeDecoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import android.util.Size as TargetSize

private val qrReader = MultiFormatReader()

class ScannerActivity : HelperBaseComponentActivity() {

    private val uiState = mutableStateOf(ScannerUiState.IDLE)

    // Свой слой у экрана уже есть - в него пишется картинка камеры, и её размывают
    // стеклянные капсулы управления. Второй, общий, тут не нужен
    override val recordGlassBackdrop: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startScan()
    }

    @Composable
    override fun ScreenContent() {
        ScannerScreen(
            uiState = uiState.value,
            onBackClick = { finish() },
            onSelectPhoto = { showFileChooser() },
            onStartScan = { startScan() },
            onStopScan = { stopScan() },
            onScanResult = { text -> finished(text) }
        )
    }

    private fun startScan() {
        checkAndRequestPermission(PermissionType.CAMERA) {
            uiState.value = ScannerUiState.ACTIVE
        }
    }

    private fun stopScan() {
        uiState.value = ScannerUiState.IDLE
    }

    /**
     * Распознанный код приходит с потока разбора кадров, а закрывать экран и отдавать
     * результат можно только с главного. Раньше это делалось прямо оттуда: код читался,
     * а экран не закрывался и результат никуда не уходил.
     */
    private fun finished(text: String) {
        runOnUiThread {
            val intent = Intent()
            intent.putExtra("SCAN_RESULT", text)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun showFileChooser() {
        launchFileChooser("image/*") { uri ->
            if (uri == null) return@launchFileChooser
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                val text = QRCodeDecoder.syncDecodeQRCode(bitmap)
                if (text.isNullOrEmpty()) {
                    toast(R.string.toast_decoding_failed)
                } else {
                    finished(text)
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to decode QR code from file", e)
                toast(R.string.toast_decoding_failed)
            }
        }
    }
}

enum class ScannerUiState {
    IDLE,
    ACTIVE
}

/**
 * Что происходит с кадрами прямо сейчас.
 *
 * Временно и намеренно на виду. Сканер уже дважды чинился вслепую, а журнал
 * приложения по умолчанию отбрасывает всё ниже уровня «warning», так что записи в
 * него не помогали. По этим трём числам сразу видно, где обрыв: нет кадров - молчит
 * камера, кадры есть и попыток разбора столько же - не находится код, есть признак
 * распознавания - ломается возврат результата.
 */
class ScannerStats {
    var frames by mutableIntStateOf(0)
    var frameSize by mutableStateOf("")
    var recognized by mutableStateOf(false)
}

@Composable
fun ScannerScreen(
    uiState: ScannerUiState,
    onBackClick: () -> Unit,
    onSelectPhoto: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onScanResult: (String) -> Unit
) {
    val isScanning = uiState == ScannerUiState.ACTIVE
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var hasTorch by remember { mutableStateOf(false) }
    var torchEnabled by rememberSaveable { mutableStateOf(false) }

    // Слой с картинкой камеры: его и размывают стеклянные капсулы управления.
    // Общий слой темы тут не годится - капсулы сами в него пишутся
    val previewBackdrop = rememberGlassBackdrop()
    val stats = remember { ScannerStats() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (uiState) {
            ScannerUiState.IDLE -> ScannerIdlePlaceholder(onStartClick = onStartScan)
            ScannerUiState.ACTIVE -> {
                Box(modifier = Modifier.fillMaxSize().glassBackdropSource(previewBackdrop)) {
                    CameraXPreview(
                        onScanResult = onScanResult,
                        stats = stats,
                        onCameraReady = { control, info ->
                            cameraControl = control
                            hasTorch = info.hasFlashUnit()
                            if (torchEnabled && hasTorch) {
                                control.enableTorch(true)
                            }
                        }
                    )
                }
                ScannerFrame(modifier = Modifier.fillMaxSize())
                ScannerStatusLine(
                    stats = stats,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 260.dp)
                )
            }
        }

        // Управление плавает над камерой стеклянными капсулами - как нижняя панель на
        // главной. Обычная шапка делала из сканера отдельную страницу, хотя он
        // накладка поверх камеры, а не раздел приложения
        GlassSurface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
            shape = ScannerBarShape,
            backdrop = previewBackdrop.takeIf { isScanning },
            opaqueness = 1.1f,
            fallbackColor = Color.Black.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back_24dp),
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = stringResource(R.string.menu_item_import_config_qrcode),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
        }

        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            shape = ScannerBarShape,
            backdrop = previewBackdrop.takeIf { isScanning },
            opaqueness = 1.1f,
            fallbackColor = Color.Black.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isScanning) {
                            if (torchEnabled) {
                                torchEnabled = false
                                cameraControl?.enableTorch(false)
                            }
                            onStopScan()
                        } else {
                            onStartScan()
                        }
                    }
                ) {
                    Icon(
                        painterResource(
                            if (isScanning) R.drawable.ic_stop_24dp else R.drawable.ic_scan_24dp
                        ),
                        contentDescription = if (isScanning) "stop scan" else "start scan",
                        tint = Color.White
                    )
                }
                if (isScanning && hasTorch) {
                    IconButton(
                        onClick = {
                            torchEnabled = !torchEnabled
                            cameraControl?.enableTorch(torchEnabled)
                        }
                    ) {
                        Icon(
                            painterResource(
                                if (torchEnabled) R.drawable.ic_flash_on_24dp
                                else R.drawable.ic_flash_off_24dp
                            ),
                            contentDescription = "Torch",
                            tint = if (torchEnabled) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }
                IconButton(onClick = onSelectPhoto) {
                    Icon(
                        painterResource(R.drawable.ic_image_24dp),
                        contentDescription = "select image",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/** Форма плавающих капсул сканера. */
private val ScannerBarShape = RoundedCornerShape(28.dp)

@Composable
private fun ScannerIdlePlaceholder(onStartClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) { detectTapGestures { onStartClick() } },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_scan_24dp),
                contentDescription = "Start Scanner",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.menu_item_scan_qrcode),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.summary_scan_qrcode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Строка состояния разбора под окном наведения.
 *
 * Временная и намеренно на виду: сканер уже дважды чинился вслепую. Пока кадров нет,
 * говорит об этом прямо - значит молчит камера, а не разбор.
 */
@Composable
private fun ScannerStatusLine(stats: ScannerStats, modifier: Modifier = Modifier) {
    val text = when {
        stats.recognized -> "код найден"
        stats.frames == 0 -> "нет кадров с камеры"
        else -> "кадры: ${stats.frames} · ${stats.frameSize}"
    }
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/**
 * Окно наведения: затемнение вокруг и уголки по краю.
 *
 * Затемнение рисуется четырьмя полосами вокруг окна, а не сплошным прямоугольником с
 * вырезом через BlendMode.Clear, как было раньше. Clear вычищает пиксели окна
 * приложения, и что окажется под ними, зависит от того, как собирается кадр и каким
 * способом выводится камера: где-то проступала картинка, где-то оставалась дыра.
 * Четыре полосы дают тот же вид и ни от чего не зависят.
 */
@Composable
private fun ScannerFrame(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val box = minOf(size.width, size.height) * 0.66f
        val left = (size.width - box) / 2f
        val top = (size.height - box) / 2f
        val dim = Color.Black.copy(alpha = 0.5f)

        drawRect(dim, size = Size(size.width, top))
        drawRect(
            dim,
            topLeft = Offset(0f, top + box),
            size = Size(size.width, size.height - top - box)
        )
        drawRect(dim, topLeft = Offset(0f, top), size = Size(left, box))
        drawRect(
            dim,
            topLeft = Offset(left + box, top),
            size = Size(size.width - left - box, box)
        )

        // Уголки: восемь отрезков по углам окна
        val len = box * 0.13f
        val stroke = 4.dp.toPx()
        val right = left + box
        val bottom = top + box
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(accent, Offset(x, y), Offset(x + dx, y), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(accent, Offset(x, y), Offset(x, y + dy), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        corner(left, top, len, len)
        corner(right, top, -len, len)
        corner(left, bottom, len, -len)
        corner(right, bottom, -len, -len)
    }
}

@Composable
fun CameraXPreview(
    onScanResult: (String) -> Unit,
    onCameraReady: (CameraControl, CameraInfo) -> Unit,
    stats: ScannerStats
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val foundResult = remember { AtomicBoolean(false) }
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }

    DisposableEffect(lifecycleOwner) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        var cameraProvider: ProcessCameraProvider? = null
        var camera: Camera? = null

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    TargetSize(1280, 720),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(analysisExecutor) { imageProxy ->
                    // Пишем состояние до разбора: если разбор упадёт, кадр всё равно
                    // будет засчитан, и станет видно, что камера работает
                    stats.frames += 1
                    stats.frameSize = "${imageProxy.width}x${imageProxy.height}"
                    if (stats.frames % 60 == 1) {
                        // Уровень «warn», а не «info»: журнал по умолчанию режет всё
                        // ниже него, и записи попросту не доходили
                        LogUtil.w(AppConfig.TAG, "Scanner: frame #${stats.frames} ${stats.frameSize}")
                    }
                    processImageProxy(imageProxy, foundResult) { text ->
                        stats.recognized = true
                        onScanResult(text)
                    }
                }
            }

        val preview = Preview.Builder().build()
        preview.setSurfaceProvider { request -> surfaceRequest = request }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                try {
                    cameraProvider = cameraProviderFuture.get()
                    cameraProvider?.unbindAll()
                    camera = cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    camera?.let {
                        onCameraReady(it.cameraControl, it.cameraInfo)
                    }
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "CameraX bind failed", e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            cameraProvider?.unbindAll()
            analysisExecutor.shutdownNow()
        }
    }

    surfaceRequest?.let { request ->
        val coordinateTransformer = remember { MutableCoordinateTransformer() }
        CameraXViewfinder(
            surfaceRequest = request,
            // Картинку рисует сам Compose, а не отдельная поверхность под окном.
            // С отдельной поверхностью её не видит ни стекло, ни слой фона, и окно
            // наведения приходилось прорезать насквозь через BlendMode.Clear
            implementationMode = ImplementationMode.EMBEDDED,
            coordinateTransformer = coordinateTransformer,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Одна попытка распознавания. Читатель хранит состояние, поэтому сбрасываем его. */
private fun decodeOrNull(
    source: com.google.zxing.LuminanceSource,
    hints: Map<DecodeHintType, Any>
): String? = try {
    qrReader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
} catch (_: Exception) {
    null
} finally {
    qrReader.reset()
}

private fun processImageProxy(
    imageProxy: ImageProxy,
    foundResult: AtomicBoolean,
    onResult: (String) -> Unit
) {
    if (foundResult.get()) {
        imageProxy.close()
        return
    }
    try {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val width = imageProxy.width
        val height = imageProxy.height
        // Ширина строки в буфере больше ширины кадра, когда камера добивает строки до
        // кратности. Брать вместо неё ширину кадра нельзя: картинка расползается по
        // диагонали, и декодер не находит в ней ничего никогда
        val rowStride = plane.rowStride

        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )

        // Сначала середина кадра - в неё и наводят по рамке. На весь кадр бинаризатор
        // считает порог по всей картинке разом, и код, занимающий её малую часть,
        // тонет в фоне. Полный кадр остаётся второй попыткой: код может быть и крупным
        val side = minOf(width, height) * 2 / 3
        val text = decodeOrNull(
            PlanarYUVLuminanceSource(
                bytes, rowStride, height,
                (width - side) / 2, (height - side) / 2, side, side,
                false
            ),
            hints
        ) ?: decodeOrNull(
            PlanarYUVLuminanceSource(
                bytes, rowStride, height,
                0, 0, width, height,
                false
            ),
            hints
        )

        if (!text.isNullOrEmpty() && foundResult.compareAndSet(false, true)) {
            LogUtil.w(AppConfig.TAG, "Scanner: code recognized")
            onResult(text)
        }
    } catch (_: Exception) {
        // do nothing
    } finally {
        imageProxy.close()
    }
}
