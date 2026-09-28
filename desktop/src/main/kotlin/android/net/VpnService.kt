package android.net

import android.content.Context
import android.content.Intent

/** VPN Android. На компьютере разрешения на VPN не спрашивают - prepare всегда готов. */
open class VpnService {
    companion object {
        @JvmStatic fun prepare(context: Context?): Intent? = null
    }
}
