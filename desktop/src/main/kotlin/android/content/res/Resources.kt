package android.content.res

import com.ward.desktop.AndroidResources

/** Ресурсы приложения - строки, множественные формы и массивы из res/ Android. */
class Resources internal constructor() {
    fun getString(id: Int): String = AndroidResources.string(id)
    fun getString(id: Int, vararg args: Any?): String = AndroidResources.string(id, *args)
    fun getQuantityString(id: Int, quantity: Int): String = AndroidResources.plural(id, quantity)
    fun getQuantityString(id: Int, quantity: Int, vararg args: Any?): String = AndroidResources.plural(id, quantity, *args)
    fun getStringArray(id: Int): Array<String> = AndroidResources.array(id).toTypedArray()
    fun getText(id: Int): CharSequence = getString(id)

    companion object {
        internal val instance = Resources()
    }
}
