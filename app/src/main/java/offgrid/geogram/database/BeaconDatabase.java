package offgrid.geogram.database;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import offgrid.geogram.core.Log;
import offgrid.geogram.things.BeaconReachable;

/**
 * Handles all data related to beacons that have been found
 */
public class BeaconDatabase {

    public static final String TAG = "BeaconDatabase";
    public static final String FOLDER_NAME = "beacons";
    public static final String FILE_NAME = "beacon.json";

    /**
     * Gets the base folder for beacon storage.
     *
     * @param context The application context.
     * @return The folder for storing beacon data.
     */
    public static File getFolder(Context context) {
        File folderBeacons = new File(context.getFilesDir(), FOLDER_NAME);
        if (!folderBeacons.exists()) {
            if (!folderBeacons.mkdirs()) {
                Log.e(TAG, "Failed to create folder: " + FOLDER_NAME);
                return null;
            }
        }
        return folderBeacons;
    }

    /**
     * Gets the folder associated with a specific device ID.
     *
     * @param deviceId The device ID of the beacon.
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
     * Saves a discovered beacon to the database.
     *
     * @param beacon     The beacon to save or merge.
     * @param appContext The application context.
     * @return The file where the beacon data is saved.
     */
    public static File saveOrMergeWithBeaconDiscovered(BeaconReachable beacon, Context appContext) {
        String deviceId = beacon.getDeviceId();
        if (deviceId == null) {
            Log.e(TAG, "Beacon instanceId is null");
            return null;
        }

        File folderDevice = getFolderDeviceId(deviceId, appContext);
        if (folderDevice == null) {
            return null;
        }

        File file = new File(folderDevice, FILE_NAME);
        BeaconReachable beaconFromFile = null;
        if (file.exists()) {
            beaconFromFile = BeaconReachable.fromJson(file);
            if (beaconFromFile == null) {
                Log.e(TAG, "Failed to parse beacon data from file: " + file.getAbsolutePath());
            }
        }

        if (beaconFromFile == null) {
            try {
                beacon.saveToFile(file, appContext);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save beacon data to file: " + file.getAbsolutePath() + " " + e.getMessage());
            }
        } else {
            beacon.merge(beaconFromFile);
            try {
                beacon.saveToFile(file, appContext);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save merged beacon data to file: " + file.getAbsolutePath() + " " + e.getMessage());
            }
        }

        return file;
    }

    /**
     * Retrieves a beacon by its device ID.
     *
     * @param deviceId   The device ID of the beacon.
     * @param appContext The application context.
     * @return The beacon object if found, or null if not.
     */
    public static BeaconReachable getBeacon(String deviceId, Context appContext) {
        if (deviceId == null) {
            Log.e(TAG, "Beacon deviceId is null");
            return null;
        }

        File folderDevice = getFolderDeviceId(deviceId, appContext);
        if (folderDevice == null || !folderDevice.exists()) {
            return null;
        }

        File file = new File(folderDevice, FILE_NAME);
        if (!file.exists()) {
            Log.e(TAG, "Beacon file does not exist: " + file.getAbsolutePath());
            return null;
        }

        return BeaconReachable.fromJson(file);
    }

    /**
     * Gets the list of beacons that have been discovered.
     * This list places on top the most recently found beacons.
     *
     * @param appContext The application context.
     * @return A list of discovered beacons sorted by the most recently found.
     */
    public static ArrayList<BeaconReachable> getBeacons(Context appContext) {
        ArrayList<BeaconReachable> beacons = new ArrayList<>();
        File baseFolder = getFolder(appContext);

        if (baseFolder == null || !baseFolder.exists()) {
            Log.e(TAG, "Base folder for beacons does not exist.");
            return beacons;
        }

        for (File section : baseFolder.listFiles()) {
            if (section.isDirectory()) {
                for (File deviceFolder : section.listFiles()) {
                    if (deviceFolder.isDirectory()) {
                        File beaconFile = new File(deviceFolder, FILE_NAME);
                        if (beaconFile.exists()) {
                            BeaconReachable beacon = BeaconReachable.fromJson(beaconFile);
                            if (beacon != null) {
                                beacons.add(beacon);
                            }
                        }
                    }
                }
            }
        }

        // Sort by most recently found
        beacons.sort(Comparator.comparingLong(BeaconReachable::getTimeLastFound).reversed());
        return beacons;
    }
}
