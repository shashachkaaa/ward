package com.v2ray.ang.core

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

/**
 * V2Ray Native Library Manager
 *
 * Thread-safe singleton wrapper for Libv2ray native methods.
 * Provides initialization protection and unified API for V2Ray core operations.
 */
object CoreNativeManager {
    private val initLock = Any()

    @Volatile
    private var initialized = false

    /**
     * Initialize V2Ray core environment.
     * This method is thread-safe and ensures initialization happens only once.
     *
     * Пришедший вторым не уходит ни с чем, а ждёт на замке, пока первый закончит.
     * Раньше здесь стоял флаг «занято»: второй видел его и возвращался сразу, хотя
     * библиотека ещё поднималась, - и звал ядро по неготовому окружению. Пока
     * инициализация висела в onCreate служб, разойтись двоим было негде; с
     * прогревом в стороне от главного потока это стало возможным.
     */
    fun initCoreEnv(context: Context?) {
        if (initialized) {
            LogUtil.d(AppConfig.TAG, "V2Ray core environment already initialized, skipping")
            return
        }
        synchronized(initLock) {
            if (initialized) return
            try {
                Seq.setContext(context?.applicationContext)
                val assetPath = Utils.userAssetPath(context)
                val deviceId = Utils.getDeviceIdForXUDPBaseKey()
                Libv2ray.initCoreEnv(assetPath, deviceId)
                initialized = true
                LogUtil.i(AppConfig.TAG, "V2Ray core environment initialized successfully")
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to initialize V2Ray core environment", e)
                throw e
            }
        }
    }

    /**
     * Готовит окружение ядра в стороне от главного потока.
     *
     * Инициализация подтягивает нативную библиотеку ядра и разбирает файлы карт -
     * на холодном процессе это секунды. Службе переднего плана столько занимать
     * главный поток нельзя: система отмеряет ей около десяти секунд на показ
     * уведомления, и считает их со своего startForegroundService, а не с нашего
     * onCreate. Пока инициализация стояла в onCreate, в этот срок укладывался не
     * всякий телефон, и система убивала процесс.
     *
     * Не дойти до конца тут не страшно: [initCoreEnv] идемпотентна, и тот, кому
     * ядро понадобится, позовёт её сам и дождётся на замке.
     */
    fun warmUp(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        Thread({ runCatching { initCoreEnv(appContext) } }, "core-warmup").apply {
            isDaemon = true
            start()
        }
    }

    fun reconcileBrowserDialer(dialerAddr: String) {
        try {
            Libv2ray.reconcileBrowserDialer(dialerAddr)
            LogUtil.i(AppConfig.TAG, "Browser dialer reconciled successfully with address: $dialerAddr")
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to reconcile browser dialer with address: $dialerAddr", e)
        }
    }


    /**
     * Get V2Ray core version.
     *
     * @return Version string of the V2Ray core
     */
    fun getLibVersion(): String {
        return try {
            Libv2ray.checkVersionX()
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to check V2Ray version", e)
            "Unknown"
        }
    }

    /**
     * Measure outbound connection delay.
     *
     * @param config The configuration JSON string
     * @param testUrl The URL to test against
     * @return Delay in milliseconds, or -1 if test failed
     */
    fun measureOutboundDelay(config: String, testUrl: String): Long =
        measureOutboundDelayDetailed(config, testUrl).first

    /**
     * Measure outbound connection delay, keeping the reason a failed test gives.
     *
     * @param config The configuration JSON string
     * @param testUrl The URL to test against
     * @return Delay in milliseconds and null, or -1 and the error the core reported
     */
    fun measureOutboundDelayDetailed(config: String, testUrl: String): Pair<Long, String?> {
        return try {
            Libv2ray.measureOutboundDelay(config, testUrl) to null
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to measure outbound delay", e)
            -1L to (e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName)
        }
    }

    /**
     * Create a new core controller instance.
     *
     * @param handler The callback handler for core events
     * @return A new CoreController instance
     */
    fun newCoreController(handler: CoreCallbackHandler): CoreController {
        return try {
            Libv2ray.newCoreController(handler)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to create core controller", e)
            throw e
        }
    }
}