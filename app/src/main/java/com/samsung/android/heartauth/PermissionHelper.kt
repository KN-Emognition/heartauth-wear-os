package com.samsung.android.heartauth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat


object PermissionHelper {
    @JvmStatic
    fun resolveHealthPermission(ctx: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            ctx.getString(R.string.additionalHealthDataPermission)
        } else {
            Manifest.permission.BODY_SENSORS
        }
    }

    @JvmStatic
    fun isGranted(ctx: Context, permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            ctx,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
