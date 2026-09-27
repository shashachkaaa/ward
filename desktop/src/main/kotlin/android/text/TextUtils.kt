package android.text

object TextUtils {
    @JvmStatic
    fun isEmpty(str: CharSequence?): Boolean = str.isNullOrEmpty()

    @JvmStatic
    fun equals(a: CharSequence?, b: CharSequence?): Boolean = a?.toString() == b?.toString()
}
