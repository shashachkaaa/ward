package com.v2ray.ang.extension

import com.v2ray.ang.enums.EConfigType
import java.io.Serializable
import java.net.URI
import java.util.Locale

const val THRESHOLD = 1000L
const val DIVISOR = 1024.0

/**
 * Converts a Long value to a speed string.
 *
 * @return The speed string.
 */
fun Long.toSpeedString(): String = this.toTrafficString() + "/s"

/**
 * Converts a Long value to a traffic string.
 *
 * @return The traffic string.
 */
fun Long.toTrafficString(): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    var size = this.toDouble()
    var unitIndex = 0
    while (size >= THRESHOLD && unitIndex < units.size - 1) {
        size /= DIVISOR
        unitIndex++
    }
    return String.format(Locale.getDefault(), "%.1f %s", size, units[unitIndex])
}

val URI.idnHost: String
    get() = host?.replace("[", "")?.replace("]", "").orEmpty()


/**
 * Checks if the config type is a group type (PolicyGroup or ProxyChain).
 *
 * @return True if the config type is PolicyGroup or ProxyChain, false otherwise.
 */
fun EConfigType.isGroupType(): Boolean {
    return this == EConfigType.POLICYGROUP || this == EConfigType.PROXYCHAIN
}

/**
 * Checks if the config type is a complex type (Custom, PolicyGroup, or ProxyChain).
 *
 * @return True if the config type is Custom, PolicyGroup, or ProxyChain, false otherwise.
 */
fun EConfigType.isComplexType(): Boolean {
    return this == EConfigType.CUSTOM || this == EConfigType.POLICYGROUP || this == EConfigType.PROXYCHAIN
}
