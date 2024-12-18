package offgrid.geogram.core;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

public class PermissionsHelper {

    private static final String TAG = "PermissionsHelper";

    // List of required permissions
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            //Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.INTERNET
    };

    private static final int PERMISSION_REQUEST_CODE = 100;

    /**
     * Requests all necessary permissions if they are not already granted.
     *
     * @param activity The activity requesting the permissions.
     * @return True if all permissions are already granted, false otherwise.
     */
    public static boolean requestPermissionsIfNecessary(Activity activity) {
        if (!hasAllPermissions(activity)) {
            Log.i(TAG, "Requesting necessary permissions...");
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            return false;
        } else {
            Log.i(TAG, "All necessary permissions already granted.");
            return true;
        }
    }

    /**
     * Checks if all necessary permissions are granted.
     *
     * @param activity The activity to check permissions for.
     * @return True if all permissions are granted, false otherwise.
     */
    public static boolean hasAllPermissions(Activity activity) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing permission: " + permission);
                return false;
            }
        }
        return true;
    }
}