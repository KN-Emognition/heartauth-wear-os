package com.samsung.android.ecgmonitor;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.samsung.android.ecgmonitor.R;

public final class PermissionHelper {

    public static String resolveHealthPermission(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            return ctx.getString(R.string.additionalHealthDataPermission);
        } else {
            return Manifest.permission.BODY_SENSORS;
        }
    }

    public static boolean isGranted(@NonNull Context ctx, @NonNull String permission) {
        return ActivityCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
