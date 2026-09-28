package android.content.pm

import android.graphics.drawable.Drawable

/** Сведения о приложении. Приложение одно - Ward. */
class PackageManager internal constructor() {
    fun getApplicationIcon(packageName: String): Drawable =
        androidx.core.content.ContextCompat.getDrawable(android.content.Context.app, com.v2ray.ang.R.mipmap.ic_launcher)!!

    fun getPackageInfo(packageName: String, flags: Int): PackageInfo = PackageInfo()

    companion object {
        internal val instance = PackageManager()
        const val GET_META_DATA = 128
    }
}

class PackageInfo {
    @JvmField val versionName: String = com.v2ray.ang.BuildConfig.VERSION_NAME
    @JvmField val packageName: String = "com.ward.client"
}
