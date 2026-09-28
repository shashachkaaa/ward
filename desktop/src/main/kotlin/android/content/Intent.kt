package android.content

import android.net.Uri
import android.os.Bundle
import java.io.Serializable

/**
 * Намерение открыть экран или передать данные. На компьютере оно живёт внутри
 * одного процесса: экран создаётся по классу, а дополнения передаются как есть.
 */
open class Intent() {
    var action: String? = null
    var data: Uri? = null
    var type: String? = null
    var component: Class<*>? = null
    var flags: Int = 0
    val extras: Bundle = Bundle()
    var clipData: ClipData? = null

    constructor(action: String?) : this() { this.action = action }
    constructor(action: String?, data: Uri?) : this() { this.action = action; this.data = data }
    constructor(context: Context?, cls: Class<*>) : this() { component = cls }
    constructor(other: Intent) : this() {
        action = other.action; data = other.data; type = other.type
        component = other.component; flags = other.flags; extras.map.putAll(other.extras.map)
    }

    fun setClass(context: Context?, cls: Class<*>): Intent = apply { component = cls }
    fun setAction(a: String?): Intent = apply { action = a }
    fun setData(u: Uri?): Intent = apply { data = u }
    fun setType(t: String?): Intent = apply { type = t }
    fun setDataAndType(u: Uri?, t: String?): Intent = apply { data = u; type = t }
    fun addFlags(f: Int): Intent = apply { flags = flags or f }
    fun setFlags(f: Int): Intent = apply { flags = f }
    fun setPackage(p: String?): Intent = this
    fun addCategory(c: String): Intent = this

    fun putExtra(key: String, value: String?): Intent = apply { extras.map[key] = value }
    fun putExtra(key: String, value: Int): Intent = apply { extras.map[key] = value }
    fun putExtra(key: String, value: Long): Intent = apply { extras.map[key] = value }
    fun putExtra(key: String, value: Boolean): Intent = apply { extras.map[key] = value }
    fun putExtra(key: String, value: Serializable?): Intent = apply { extras.map[key] = value }
    fun putExtra(key: String, value: Uri?): Intent = apply { extras.map[key] = value }
    fun putExtra(key: String, value: CharSequence?): Intent = apply { extras.map[key] = value?.toString() }
    fun putStringArrayListExtra(key: String, value: ArrayList<String>?): Intent = apply { extras.map[key] = value }
    fun putExtras(b: Bundle): Intent = apply { extras.map.putAll(b.map) }

    fun getStringExtra(key: String): String? = extras.map[key] as? String
    fun getIntExtra(key: String, default: Int): Int = extras.map[key] as? Int ?: default
    fun getLongExtra(key: String, default: Long): Long = extras.map[key] as? Long ?: default
    fun getBooleanExtra(key: String, default: Boolean): Boolean = extras.map[key] as? Boolean ?: default
    fun getSerializableExtra(key: String): Serializable? = extras.map[key] as? Serializable
    fun <T : Serializable> getSerializableExtra(key: String, clazz: Class<T>): T? = clazz.cast(extras.map[key])
    @Suppress("UNCHECKED_CAST")
    fun getStringArrayListExtra(key: String): ArrayList<String>? = extras.map[key] as? ArrayList<String>
    fun hasExtra(key: String): Boolean = extras.map.containsKey(key)

    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"
        const val ACTION_MAIN = "android.intent.action.MAIN"
        const val EXTRA_TEXT = "android.intent.extra.TEXT"
        const val EXTRA_STREAM = "android.intent.extra.STREAM"
        const val EXTRA_TITLE = "android.intent.extra.TITLE"
        const val EXTRA_SUBJECT = "android.intent.extra.SUBJECT"
        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
        const val FLAG_ACTIVITY_CLEAR_TOP = 0x04000000
        const val FLAG_ACTIVITY_SINGLE_TOP = 0x20000000
        const val FLAG_ACTIVITY_CLEAR_TASK = 0x00008000
        const val FLAG_GRANT_READ_URI_PERMISSION = 1
        const val CATEGORY_OPENABLE = "android.intent.category.OPENABLE"

        /** «Поделиться»: на компьютере делиться некуда, отдаём само намерение. */
        @JvmStatic fun createChooser(target: Intent, title: CharSequence?): Intent =
            Intent(target).apply { action = ACTION_CHOOSER }

        const val ACTION_CHOOSER = "android.intent.action.CHOOSER"
    }
}
