package com.tencent.mmkv

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.ward.desktop.DesktopPaths
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Хранилище с интерфейсом MMKV поверх JSON-файлов.
 *
 * Java-обвязки MMKV под настольную JVM нет, а весь код подписок и серверов
 * говорит с хранилищем через MmkvManager. Повторив здесь нужную ему часть
 * MMKV, MmkvManager удаётся взять без правок.
 *
 * Каждое хранилище - отдельный файл. Пишется целиком через временный файл и
 * переименование: оборванная запись оставляет прежнюю версию, а не половину.
 * Процесс один, поэтому MULTI_PROCESS_MODE ничего не значит.
 */
class MMKV private constructor(private val file: File) {

    private val lock = Any()
    private var values: JsonObject = load()

    /** Перечитать с диска - после восстановления из резервной копии. */
    internal fun reload() = synchronized(lock) { values = load() }

    private fun load(): JsonObject = try {
        if (file.exists()) JsonParser.parseString(file.readText()).asJsonObject else JsonObject()
    } catch (e: Exception) {
        // Битый файл не должен ронять приложение: откладываем его в сторону
        file.renameTo(File(file.path + ".broken-" + System.currentTimeMillis()))
        JsonObject()
    }

    private fun save() {
        val tmp = File(file.path + ".tmp")
        tmp.writeText(gson.toJson(values))
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun put(key: String, value: JsonElement?): Boolean = synchronized(lock) {
        if (value == null) values.remove(key) else values.add(key, value)
        save()
        true
    }

    private fun get(key: String): JsonElement? = synchronized(lock) { values.get(key) }

    fun encode(key: String, value: String?): Boolean = put(key, value?.let { JsonPrimitive(it) })
    fun encode(key: String, value: Int): Boolean = put(key, JsonPrimitive(value))
    fun encode(key: String, value: Long): Boolean = put(key, JsonPrimitive(value))
    fun encode(key: String, value: Float): Boolean = put(key, JsonPrimitive(value))
    fun encode(key: String, value: Boolean): Boolean = put(key, JsonPrimitive(value))
    fun encode(key: String, value: Set<String>?): Boolean = put(key, value?.let { gson.toJsonTree(it) })

    fun decodeString(key: String): String? = decodeString(key, null)
    fun decodeString(key: String, defaultValue: String?): String? {
        val v = get(key) ?: return defaultValue
        return if (v.isJsonPrimitive) v.asString else defaultValue
    }

    fun decodeInt(key: String, defaultValue: Int): Int = number(key)?.toInt() ?: defaultValue
    fun decodeLong(key: String, defaultValue: Long): Long = number(key)?.toLong() ?: defaultValue
    fun decodeFloat(key: String, defaultValue: Float): Float = number(key)?.toFloat() ?: defaultValue

    fun decodeBool(key: String, defaultValue: Boolean): Boolean {
        val v = get(key) ?: return defaultValue
        return try {
            v.asJsonPrimitive.let { if (it.isBoolean) it.asBoolean else it.asString.toBooleanStrict() }
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun decodeStringSet(key: String): MutableSet<String>? {
        val v = get(key) ?: return null
        return if (v.isJsonArray) v.asJsonArray.map { it.asString }.toMutableSet() else null
    }

    fun containsKey(key: String): Boolean = get(key) != null
    fun remove(key: String) { put(key, null) }
    fun removeValueForKey(key: String) = remove(key)
    fun allKeys(): Array<String>? = synchronized(lock) { values.keySet().toTypedArray() }
    fun clearAll() = synchronized(lock) {
        values.keySet().toList().forEach { values.remove(it) }
        save()
    }

    // Настройки на Android хранят числа и строкой, и числом: читаем оба вида
    private fun number(key: String): Number? {
        val v = get(key) ?: return null
        return try {
            val p = v.asJsonPrimitive
            if (p.isNumber) p.asNumber else p.asString.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val SINGLE_PROCESS_MODE = 1
        const val MULTI_PROCESS_MODE = 2

        private val gson = Gson()
        private val instances = HashMap<String, MMKV>()

        @JvmStatic
        fun mmkvWithID(id: String, mode: Int = SINGLE_PROCESS_MODE): MMKV = synchronized(instances) {
            instances.getOrPut(id) {
                val dir = File(DesktopPaths.dataDir, "store").apply { mkdirs() }
                MMKV(File(dir, "$id.json"))
            }
        }

        @JvmStatic
        fun defaultMMKV(): MMKV = mmkvWithID("DEFAULT")

        private val storeDir get() = File(DesktopPaths.dataDir, "store")

        /**
         * Резервная копия всех хранилищ в папку. Копия настольная: файлы MMKV с
         * Android в ней не читаются, и наоборот - форматы у них разные.
         */
        @JvmStatic
        fun backupAllToDirectory(dir: String): Int {
            val target = File(dir).apply { mkdirs() }
            val files = storeDir.listFiles { f -> f.name.endsWith(".json") }.orEmpty()
            files.forEach { it.copyTo(File(target, it.name), overwrite = true) }
            return files.size
        }

        @JvmStatic
        fun restoreAllFromDirectory(dir: String): Int {
            val files = File(dir).walkTopDown().filter { it.isFile && it.name.endsWith(".json") }.toList()
            files.forEach { it.copyTo(File(storeDir, it.name), overwrite = true) }
            synchronized(instances) { instances.values.forEach { it.reload() } }
            return files.size
        }
    }
}
