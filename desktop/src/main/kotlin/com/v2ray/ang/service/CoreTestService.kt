package com.v2ray.ang.service

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.RealPingEvent
import com.v2ray.ang.dto.TestServiceMessage
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.util.LogUtil
import java.util.Collections

/**
 * Проверка серверов. На Android это отдельная служба с уведомлением; здесь -
 * объект с той же логикой: запускает RealPingWorkerService и пересылает его
 * события интерфейсу теми же сообщениями.
 */
object CoreTestService {

    private val activeWorkers = Collections.synchronizedList(mutableListOf<RealPingWorkerService>())

    fun handle(context: Context, message: TestServiceMessage) {
        when (message.key) {
            AppConfig.MSG_MEASURE_CONFIG_START -> start(context, message)
            AppConfig.MSG_MEASURE_CONFIG_CANCEL -> cancelAll()
        }
    }

    private fun start(context: Context, message: TestServiceMessage) {
        val guids = when {
            message.serverGuids.isNotEmpty() -> message.serverGuids
            message.subscriptionId.isNotEmpty() -> MmkvManager.decodeServerList(message.subscriptionId)
            else -> MmkvManager.decodeAllServerList()
        }
        if (guids.isEmpty()) return
        LogUtil.i(AppConfig.TAG, "CoreTestService: testing ${guids.size} servers")
        lateinit var worker: RealPingWorkerService
        worker = RealPingWorkerService(context, guids, message.onlyTcp) { event ->
            when (event) {
                is RealPingEvent.Progress ->
                    MessageHelper.sendMsg2UI(context, AppConfig.MSG_MEASURE_CONFIG_NOTIFY, event.text)
                is RealPingEvent.Result -> {
                    MmkvManager.encodeServerTestDelayMillis(event.guid, event.delayMillis)
                    MessageHelper.sendMsg2UI(context, AppConfig.MSG_MEASURE_CONFIG_SUCCESS, event.guid)
                }
                is RealPingEvent.Finish -> {
                    MessageHelper.sendMsg2UI(context, AppConfig.MSG_MEASURE_CONFIG_FINISH, event.status)
                    activeWorkers.remove(worker)
                }
            }
        }
        activeWorkers += worker
        worker.start()
    }

    fun cancelAll() {
        ArrayList(activeWorkers).forEach { it.cancel() }
        activeWorkers.clear()
    }
}
