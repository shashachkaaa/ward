package com.v2ray.ang.handler

import android.net.VpnService
import android.os.Build
import com.v2ray.ang.AppConfig

/**
 * Включён ли у приложения постоянный VPN и блокирует ли он трафик мимо туннеля.
 *
 * Разница между этими двумя не косметическая. Постоянный VPN сам по себе только
 * поднимает туннель заново; пока он поднимается - а после перезагрузки или обрыва это
 * секунды, - трафик идёт открытым, и человек об этом не узнает. Блокировка закрывает
 * именно эту щель: без туннеля наружу не уходит ничего.
 *
 * Спросить об этом может только сам работающий сервис - системного способа узнать своё
 * состояние со стороны у приложения нет. Отсюда два следствия. Первое: пока туннель не
 * поднят, ответа нет вовсе, поэтому последний известный мы запоминаем. Второе: ответ
 * приходится передавать из процесса сервиса в процесс интерфейса тем же каналом, что и
 * замер скорости, - они живут в разных процессах.
 */
data class LockdownStatus(
    /** Система сама поднимает туннель и держит его поднятым. */
    val alwaysOn: Boolean,
    /** Без туннеля трафик наружу не выпускается. */
    val lockdown: Boolean
) {

    /** Защищён ли человек от утечки в момент обрыва. */
    val isSealed: Boolean get() = alwaysOn && lockdown

    fun encode(): String = "${alwaysOn.toInt()};${lockdown.toInt()}"

    companion object {

        /**
         * Спрашивает у работающего сервиса.
         *
         * До Android 10 этих вопросов системе не задать, и притворяться, что ответ
         * «нет», нельзя: «выключено» и «не спросить» - разные вещи, и вторая не повод
         * пугать человека надписью о незащищённом трафике.
         */
        fun of(service: VpnService): LockdownStatus? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            return LockdownStatus(
                alwaysOn = service.isAlwaysOn,
                lockdown = service.isLockdownEnabled
            )
        }

        fun decode(raw: String): LockdownStatus? {
            val parts = raw.split(";")
            if (parts.size != 2) return null
            return LockdownStatus(
                alwaysOn = parts[0].trim() == "1",
                lockdown = parts[1].trim() == "1"
            )
        }

        /** Последнее, что мы знали. Пока туннель не поднят, спросить больше не у кого. */
        fun remembered(): LockdownStatus? =
            MmkvManager.decodeSettingsString(AppConfig.PREF_LOCKDOWN_STATUS)
                ?.let { decode(it) }

        fun remember(status: LockdownStatus) {
            MmkvManager.encodeSettings(AppConfig.PREF_LOCKDOWN_STATUS, status.encode())
        }

        private fun Boolean.toInt(): Int = if (this) 1 else 0
    }
}
