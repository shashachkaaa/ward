package android.database

/** Одна строка сведений о файле - то, о чём экраны спрашивают ContentResolver.query. */
class Cursor internal constructor(private val row: Map<String, Any?>) : java.io.Closeable {
    private val columns = row.keys.toList()
    fun moveToFirst(): Boolean = true
    fun getColumnIndex(name: String): Int = columns.indexOf(name)
    fun getColumnIndexOrThrow(name: String): Int = getColumnIndex(name).also { require(it >= 0) { name } }
    fun getString(index: Int): String? = row[columns[index]]?.toString()
    fun getLong(index: Int): Long = (row[columns[index]] as? Number)?.toLong() ?: 0L
    override fun close() {}
}
