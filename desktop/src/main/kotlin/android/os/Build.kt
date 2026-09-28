package android.os

/** Версия «системы» для скопированного кода: считаем её самой новой, ветки для старых Android не нужны. */
object Build {
    object VERSION {
        @JvmField val SDK_INT = 36
        @JvmField val RELEASE: String = System.getProperty("os.name") + " " + System.getProperty("os.version")
    }

    object VERSION_CODES {
        const val M = 23
        const val N = 24
        const val O = 26
        const val P = 28
        const val Q = 29
        const val R = 30
        const val S = 31
        const val TIRAMISU = 33
        const val UPSIDE_DOWN_CAKE = 34
        const val VANILLA_ICE_CREAM = 35
        const val BAKLAVA = 36
        const val CINNAMON_BUN = 37
    }

    @JvmField val SUPPORTED_ABIS: Array<String> = arrayOf(System.getProperty("os.arch") ?: "x86_64")
    @JvmField val MANUFACTURER: String = System.getProperty("os.name") ?: ""
    @JvmField val MODEL: String = System.getProperty("os.name") ?: ""
}
