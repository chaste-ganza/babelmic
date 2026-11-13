package com.babelmic.app.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Convenience wrapper for requesting the runtime permissions BabelMic needs.
 *
 * <p>Android 13+ requires {@code NEARBY_WIFI_DEVICES} instead of
 * {@code ACCESS_FINE_LOCATION} for Wi-Fi Direct. This helper handles both
 * cases transparently.</p>
 */
public final class PermissionHelper {

    public static final int REQUEST_CODE_ALL_PERMISSIONS = 100;

    private PermissionHelper() {}

    /**
     * @return true if every required permission has already been granted.
     */
    public static boolean hasAllPermissions(Context context) {
        return getMissingPermissions(context).isEmpty();
    }

    /**
     * Request any permissions that have not yet been granted.
     * Result arrives in {@code Activity.onRequestPermissionsResult()}.
     */
    public static void requestMissingPermissions(Activity activity) {
        List<String> missing = getMissingPermissions(activity);
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    missing.toArray(new String[0]),
                    REQUEST_CODE_ALL_PERMISSIONS);
        }
    }

    /**
     * @return List of permission strings that have not yet been granted.
     */
    public static List<String> getMissingPermissions(Context context) {
        List<String> missing = new ArrayList<>();

        // Always required
        checkAndAdd(context, Manifest.permission.RECORD_AUDIO,          missing);
        checkAndAdd(context, Manifest.permission.ACCESS_WIFI_STATE,     missing);
        checkAndAdd(context, Manifest.permission.CHANGE_WIFI_STATE,     missing);

        // Android 13+ uses NEARBY_WIFI_DEVICES; older versions need ACCESS_FINE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndAdd(context, Manifest.permission.NEARBY_WIFI_DEVICES, missing);
        } else {
            checkAndAdd(context, Manifest.permission.ACCESS_FINE_LOCATION, missing);
        }

        return missing;
    }

    /**
     * Interpret the result of {@code onRequestPermissionsResult}.
     *
     * @return true if all requested permissions were granted.
     */
    public static boolean allGranted(int[] grantResults) {
        if (grantResults.length == 0) return false;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static void checkAndAdd(Context ctx, String permission, List<String> list) {
        if (ContextCompat.checkSelfPermission(ctx, permission)
                != PackageManager.PERMISSION_GRANTED) {
            list.add(permission);
        }
    }
}
