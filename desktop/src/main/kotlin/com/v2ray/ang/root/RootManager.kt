package com.v2ray.ang.root

/** Настольная версия: режима root нет. */
object RootManager {
    fun cachedRoot(): Boolean = false
    fun isRootAvailable(forceRefresh: Boolean = false): Boolean = false
    suspend fun refresh(): Boolean = false
}
