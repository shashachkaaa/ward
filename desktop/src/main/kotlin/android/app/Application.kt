package android.app

import android.content.Context

open class Application : Context() {
    override val applicationContext: Context get() = this

    companion object {
        /** Одно приложение на процесс, как на Android. */
        val instance: Application get() = com.v2ray.ang.AngApplication.application
    }
}
