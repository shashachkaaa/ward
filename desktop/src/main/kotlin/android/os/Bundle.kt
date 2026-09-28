package android.os

import java.io.Serializable

/** Набор значений для Intent. Хранится как есть, без сериализации: процесс один. */
open class Bundle() {
    internal val map = LinkedHashMap<String, Any?>()

    constructor(other: Bundle) : this() { map.putAll(other.map) }

    fun putString(key: String, value: String?) { map[key] = value }
    fun putInt(key: String, value: Int) { map[key] = value }
    fun putLong(key: String, value: Long) { map[key] = value }
    fun putBoolean(key: String, value: Boolean) { map[key] = value }
    fun putSerializable(key: String, value: Serializable?) { map[key] = value }
    fun putStringArrayList(key: String, value: ArrayList<String>?) { map[key] = value }

    fun getString(key: String): String? = map[key] as? String
    fun getString(key: String, default: String): String = map[key] as? String ?: default
    fun getInt(key: String, default: Int = 0): Int = map[key] as? Int ?: default
    fun getLong(key: String, default: Long = 0): Long = map[key] as? Long ?: default
    fun getBoolean(key: String, default: Boolean = false): Boolean = map[key] as? Boolean ?: default
    @Suppress("UNCHECKED_CAST")
    fun getStringArrayList(key: String): ArrayList<String>? = map[key] as? ArrayList<String>
    fun getSerializable(key: String): Serializable? = map[key] as? Serializable
    fun <T : Serializable> getSerializable(key: String, clazz: Class<T>): T? = clazz.cast(map[key])
    fun containsKey(key: String): Boolean = map.containsKey(key)
    fun remove(key: String) { map.remove(key) }
    fun keySet(): Set<String> = map.keys
    val isEmpty: Boolean get() = map.isEmpty()
}
