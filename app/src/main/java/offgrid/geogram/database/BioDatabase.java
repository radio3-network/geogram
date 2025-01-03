package offgrid.geogram.database;

import android.content.Context;

import java.io.File;
import java.util.HashMap;

import offgrid.geogram.core.Log;

/**
 * Handles all data related to bio profiles that were sent
 * Each bio is mapped to a Device ID
 */
public class BioDatabase {

    public static final String TAG = "BioDatabase";
    public static final String FOLDER_NAME = "bios";
    public static final String FILE_NAME = "bio.json";

    // the most up to date list of beacons saved on our disk
    public static HashMap<String, BioProfile> bios = new HashMap<>();

    /**
     * Gets the base folder for storage.
     *
     * @param context The application context.
     * @return The folder for storing beacon data.
     */
    public static File getFolder(Context context) {
        File folder = new File(context.getFilesDir(), FOLDER_NAME);
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Log.e(TAG, "Failed to create folder: " + FOLDER_NAME);
                return null;
            }
        }
        return folder;
    }

    /**
     * Gets the folder associated with a specific device ID.
     *
     * @param deviceId The device ID
     * @param context  The application context.
     * @return The folder for the specified device ID.
     */
    public static File getFolderDeviceId(String deviceId, Context context) {
        if (deviceId == null) {
            Log.e(TAG, "Device ID is null");
            return null;
        }

        if (deviceId.length() < 3) {
            Log.e(TAG, "Device ID is too short: " + deviceId);
            return null;
        }
        String id = deviceId.substring(0, 3); // First three characters
        File folderBase = getFolder(context);
        if (folderBase == null) {
            Log.e(TAG, "Base folder could not be created");
            return null;
        }
        File folderSection = new File(folderBase, id);
        File folderDevice = new File(folderSection, deviceId);

        if (!folderDevice.exists()) {
            if (!folderDevice.mkdirs()) {
                Log.e(TAG, "Failed to create folder for device ID: " + folderDevice.getAbsolutePath());
                return null;
            }
        }
        return folderDevice;
    }

    /**
     * Saves a discovered thing to the database.
     *
     * @param profile     The profile to save or merge.
     * @param appContext The application context.
     */
    public static void saveOrMergeWithBeaconDiscovered(BioProfile profile, Context appContext) {
        String deviceId = profile.getDeviceId();
        if (deviceId == null) {
            Log.e(TAG, "Device Id is null");
            return;
        }

        File folderDevice = getFolderDeviceId(deviceId, appContext);
        if (folderDevice == null) {
            return;
        }

        File file = new File(folderDevice, FILE_NAME);
        BioProfile bioProfileFromFile = null;
        if (file.exists()) {
            bioProfileFromFile = BioProfile.fromJson(file);
            if (bioProfileFromFile == null) {
                Log.e(TAG,
                        "Failed to parse beacon data from file: "
                        + file.getAbsolutePath());
            }
        }

        if (bioProfileFromFile == null) {
            try {
                profile.saveToFile(file, appContext);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save data to file: " + file.getAbsolutePath() + " " + e.getMessage());
            }
        } else {
            profile.merge(bioProfileFromFile);
            try {
                profile.saveToFile(file, appContext);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save merged data to file: " + file.getAbsolutePath() + " " + e.getMessage());
            }
        }
        Log.i(TAG, "Saved data to file: " + file.getAbsolutePath());
        return;
    }

    /**
     * Retrieves a beacon by its device ID.
     *
     * @param deviceId   The device ID of the beacon.
     * @param appContext The application context.
     * @return The beacon object if found, or null if not.
     */
    public static BioProfile getBio(String deviceId, Context appContext) {
        if (deviceId == null) {
            Log.e(TAG, "Device Id is null");
            return null;
        }

        File folderDevice = getFolderDeviceId(deviceId, appContext);
        if (folderDevice == null || !folderDevice.exists()) {
            return null;
        }

        File file = new File(folderDevice, FILE_NAME);
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: " + file.getAbsolutePath());
            return null;
        }
        return BioProfile.fromJson(file);
    }

}
