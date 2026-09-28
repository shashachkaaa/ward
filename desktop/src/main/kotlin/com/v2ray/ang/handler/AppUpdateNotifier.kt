package com.v2ray.ang.handler

import android.content.Context
import com.v2ray.ang.AngApplication

/**
 * Уведомление о новой версии. Настольная версия: проверка идёт при запуске
 * (UpdateCheckerManager.checkQuietly), а найденное обновление показывает плашка
 * в окне - фоновой задачи и системного уведомления не нужно.
 */
object AppUpdateNotifier {
    fun schedule(context: Context = AngApplication.application) {}
    fun cancel(context: Context = AngApplication.application) {}
    fun notifyUpdate(context: Context, version: String, downloadUrl: String?) {}
}
