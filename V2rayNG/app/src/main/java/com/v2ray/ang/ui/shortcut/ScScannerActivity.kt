package com.v2ray.ang.ui.shortcut

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.BatchImportResult
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.ui.base.HelperBaseComponentActivity
import com.v2ray.ang.ui.main.MainActivity
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScScannerActivity : HelperBaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        LaunchedEffect(Unit) {
            importQRcode()
        }
    }

    private fun importQRcode() {
        launchQRCodeScanner { scanResult ->
            if (scanResult == null) {
                finish()
                return@launchQRCodeScanner
            }

            // Импорт уходит с главного потока. Если в коде подписка, он её сразу
            // скачивает, а это сетевой запрос - и прямо в обработчике камеры,
            // на главном потоке, система его запрещает. Запрос падал на первом же
            // шаге, ошибка гасилась внутри, а сюда возвращалось «подписка
            // добавлена»: человек сканировал код с ярлыка, видел успех и получал
            // пустую карточку. Каждый раз, а не изредка.
            //
            // Экран закрываем только после импорта: закрытие отменило бы его на полпути
            lifecycleScope.launch(Dispatchers.IO) {
                val imported = try {
                    // Дописываем к уже добавленным ключам, а не заменяем их
                    AngConfigManager.importBatchConfig(
                        scanResult,
                        AppConfig.STANDALONE_SUBSCRIPTION_ID,
                        true
                    )
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Failed to import scanned QR code", e)
                    BatchImportResult(0, 0)
                }
                val added = imported.count + imported.countSub

                withContext(Dispatchers.Main) {
                    when {
                        added == 0 -> toastError(R.string.toast_failure)
                        imported.hasSubFailures -> toastError(R.string.import_subscription_not_downloaded)
                        else -> toastSuccess(R.string.toast_success)
                    }
                    startActivity(Intent(this@ScScannerActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}