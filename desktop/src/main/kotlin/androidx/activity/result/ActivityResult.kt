package androidx.activity.result

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

data class ActivityResult(val resultCode: Int, val data: Intent?)

abstract class ActivityResultLauncher<I> {
    abstract fun launch(input: I)
    fun unregister() {}
}

fun interface ActivityResultCallback<O> {
    fun onActivityResult(result: O)
}

