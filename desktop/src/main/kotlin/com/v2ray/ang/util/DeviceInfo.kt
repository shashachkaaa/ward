package com.v2ray.ang.util

import com.v2ray.ang.AppConfig
import com.ward.desktop.DesktopPaths
import java.io.File
import java.security.MessageDigest

/**
 * Сведения об устройстве, настольная версия.
 *
 * HWID - как и на Android, идентификатор, переживающий переустановку: панель по
 * нему считает устройства в подписке. Берём идентификатор машины, который
 * система хранит сама (MachineGuid на Windows, /etc/machine-id на Linux), и
 * отдаём не его, а хэш: сам он годится и для других целей, светить его незачем.
 */
object DeviceInfo {

    const val UNKNOWN_HWID = "unknown_hwid"

    private val cachedHwid: String by lazy {
        try {
            val raw = if (DesktopPaths.isWindows) windowsMachineGuid() else linuxMachineId()
            if (raw.isNullOrBlank()) UNKNOWN_HWID else sha256(raw).take(16)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to get HWID", e)
            UNKNOWN_HWID
        }
    }

    fun hwid(): String = cachedHwid

    private fun linuxMachineId(): String? =
        listOf("/etc/machine-id", "/var/lib/dbus/machine-id")
            .map(::File).firstOrNull { it.canRead() }?.readText()?.trim()

    private fun windowsMachineGuid(): String? {
        val p = ProcessBuilder("reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid")
            .redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        return Regex("MachineGuid\\s+REG_SZ\\s+(\\S+)").find(out)?.groupValues?.get(1)
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    /** Название системы для заголовка x-device-os. */
    val osName: String get() = if (DesktopPaths.isWindows) "Windows" else "Linux"

    /** Версия системы. */
    val osVersion: String get() = System.getProperty("os.version") ?: "unknown"

    /** Модель - на компьютере её нет, отдаём имя системы целиком. */
    val model: String get() = System.getProperty("os.name") ?: osName

    val abi: String get() = System.getProperty("os.arch") ?: "unknown"
}
