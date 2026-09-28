package com.v2ray.ang.helper

import androidx.activity.ComponentActivity
import com.v2ray.ang.enums.PermissionType

/** Разрешений на компьютере не спрашивают - сразу «разрешено». */
class PermissionHelper(private val activity: ComponentActivity) {
    fun request(permissionType: PermissionType, onGranted: () -> Unit) = onGranted()
}
