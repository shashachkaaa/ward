package com.v2ray.ang.helper

import android.content.Context
import android.content.Intent
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.dto.SubscriptionUpdateMessage
import com.v2ray.ang.dto.TestServiceMessage
import com.v2ray.ang.service.CoreTestService
import com.v2ray.ang.service.SubscriptionUpdateService
import com.v2ray.ang.util.LogUtil
import java.io.Serializable

/**
 * Сообщения между интерфейсом и службами - те же, что на Android.
 *
 * Настольная версия: службы живут в том же процессе. Сообщение службе ядра
 * передаётся ей напрямую, интерфейсу - широковещательно внутри процесса, а
 * проверке и обновлению подписок - их объектам вместо запуска службы.
 */
object MessageHelper {

    fun sendMsg2Service(ctx: Context, what: Int, content: Serializable) {
        CoreServiceManager.onServiceMessage(ctx, what, content)
    }

    fun sendMsg2UI(ctx: Context, what: Int, content: Serializable) {
        sendMsg(ctx, AppConfig.BROADCAST_ACTION_ACTIVITY, what, content)
    }

    fun sendMsg2TestService(ctx: Context, message: TestServiceMessage) {
        runCatching { CoreTestService.handle(ctx, message) }
            .onFailure { LogUtil.e(AppConfig.TAG, "Failed to send message to test service", it) }
    }

    fun sendMsg2SubscriptionService(ctx: Context, message: SubscriptionUpdateMessage) {
        runCatching { SubscriptionUpdateService.handle(ctx, message) }
            .onFailure { LogUtil.e(AppConfig.TAG, "Failed to send message to subscription service", it) }
    }

    private fun sendMsg(ctx: Context, action: String, what: Int, content: Serializable) {
        val intent = Intent()
        intent.action = action
        intent.putExtra("key", what)
        intent.putExtra("content", content)
        ctx.sendBroadcast(intent)
    }
}
