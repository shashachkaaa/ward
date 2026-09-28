package android

/** Имена разрешений Android: на компьютере их никто не спрашивает, но код ссылается на них. */
object Manifest {
    object permission {
        const val CAMERA = "android.permission.CAMERA"
        const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
        const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
