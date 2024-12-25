package offgrid.geogram.database;

import android.content.Context;

import java.io.File;

import offgrid.geogram.core.Log;

/**
 * Handles all data related to beacons that have been found
 */
public class BeaconDatabase {

    public static final String TAG = "BeaconDatabase";
    public static final String FOLDER_NAME = "beacons";

    public static File getFolder(Context context){
        File folderBeacons = new File(context.getFilesDir(), FOLDER_NAME);
        if (!folderBeacons.exists()) {
            if (!folderBeacons.mkdirs()) {
                Log.e(TAG, "Failed to create folder: " + FOLDER_NAME);
            }
        }
        return folderBeacons;
    }

}
