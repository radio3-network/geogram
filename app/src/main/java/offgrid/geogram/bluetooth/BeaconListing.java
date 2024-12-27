package offgrid.geogram.bluetooth;

import static offgrid.geogram.bluetooth.BluetoothCentral.EDDYSTONE_SERVICE_UUID;
import static offgrid.geogram.bluetooth.BluetoothUtils.calculateDistance;
import static offgrid.geogram.util.DateUtils.getHumanReadableTime;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

import offgrid.geogram.MainActivity;
import offgrid.geogram.R;
import offgrid.geogram.database.BeaconDatabase;
import offgrid.geogram.things.BeaconReachable;
import offgrid.geogram.core.Log;
import offgrid.geogram.fragments.BeaconDetailsFragment;

/**
 * Manages the list of beacons that were found
 * and lists them on the UI of the android app
 */
public class BeaconListing {

    private static final String TAG = "BeaconList";

    private static BeaconListing instance; // Singleton instance

    // When was the window last updated?
    private long lastUpdated = System.currentTimeMillis();

    // list of beacons both reachable and past ones
    //public final HashMap<String, BeaconReachable> beacons = new HashMap<>();

    // Private constructor for Singleton pattern
    private BeaconListing() {
        // Prevent instantiation
    }

    /**
     * Singleton access to the BeaconListing instance.
     */
    public static synchronized BeaconListing getInstance() {
        if (instance == null) {
            instance = new BeaconListing();
        }
        return instance;
    }
//
//    /**
//     * Processes a beacon scan result. This is called automatically
//     * from the callback in BeaconFinder whenever a beacon is found,
//     * even if the beacon was seen before.
//     */
//    public void processBeacon(ScanResult result, Context context) {
//
//        // Only update after some time
//        long timeNow = System.currentTimeMillis();
//        long timePassed = timeNow - lastUpdated;
//        if (timePassed < 2000) {
//            return;
//        }
//        lastUpdated = timeNow;
//
//        boolean canUpdateList = false;
//
//        // Get basic data from the beacon
//        String deviceAddress = result.getDevice().getAddress();
//        int rssi = result.getRssi();
//
//        // Get advanced data
//        String namespaceId = null;
//        String deviceId = null;
//        byte[] serviceData = Objects.requireNonNull(result.getScanRecord())
//                .getServiceData(EDDYSTONE_SERVICE_UUID);
//
//        // Skip if no service data
//        if (serviceData == null) {
//            return;
//        } else {
//            namespaceId = extractNamespaceId(serviceData);
//            deviceId = extractInstanceId(serviceData);
//        }
//
//
//        // Find an existing beacon of this kind
//        BeaconReachable beacon = null;
//
//        // beacons that are within radio reach
//        Collection<BeaconReachable> beaconsExisting
//                = BeaconFinder.getInstance(context).getBeaconMap().values();
//
//        for (BeaconReachable b : beaconsExisting) {
//            if (b.getDeviceId().equals(deviceId)) {
//                beacon = b;
//                beacon.setMacAddress(deviceAddress);
//                beacon.setTimeLastFound(System.currentTimeMillis());
//                break;
//            }
//        }
//
////        // Create a new beacon if none found
////        if (beacon == null) {
////            beacon = new BeaconReachable();
////            beacon.setMacAddress(deviceAddress);
////            beaconsDiscovered.add(beacon);
////            Log.i(TAG, "Beacon found: " + deviceAddress + ", RSSI: " + rssi);
////            canUpdateList = true;
////        } else {
////            // Update the timestamp of last seen
////            beacon.setTimeLastFound(System.currentTimeMillis());
////        }
//
//        // Update the RSSI value
//        if (rssi != beacon.getRssi()) {
//            beacon.setRssi(rssi);
//            canUpdateList = true;
//        }
//
//        if (beacon.getDeviceId() == null) {
//            beacon.setNamespaceId(namespaceId);
//            beacon.setInstanceId(deviceId);
//            canUpdateList = true;
//        }
//
//        // Update the service data if it changed
//        if (Arrays.hashCode(serviceData) != Arrays.hashCode(beacon.getServiceData())) {
//            beacon.setServiceData(serviceData);
//            canUpdateList = true;
//        }
//
//        // Save to disk
//        BeaconDatabase.saveOrMergeWithBeaconDiscovered(beacon, context);
//
//        // Update the GUI showing the beacons
//        if (canUpdateList) {
//            updateList();
//        }
//    }
//
//    /**
//     * Extracts the Namespace ID from service data.
//     */
//    private String extractNamespaceId(byte[] serviceData) {
//        if (serviceData == null || serviceData.length < 18) { // Ensure valid service data length
//            Log.e(TAG, "Invalid service data for Eddystone UID.");
//            return "Unknown";
//        }
//
//        byte[] namespaceIdBytes = Arrays.copyOfRange(serviceData, 2, 12);
//        StringBuilder namespaceId = new StringBuilder();
//        for (byte b : namespaceIdBytes) {
//            namespaceId.append(String.format("%02X", b));
//        }
//        return namespaceId.toString();
//    }
//
//    /**
//     * Extracts the Instance ID from service data.
//     */
//    private String extractInstanceId(byte[] serviceData) {
//        if (serviceData == null || serviceData.length < 18) { // Ensure valid service data length
//            Log.e(TAG, "Invalid service data for Eddystone UID.");
//            return "Unknown";
//        }
//
//        byte[] instanceIdBytes = Arrays.copyOfRange(serviceData, 12, 18);
//        StringBuilder instanceId = new StringBuilder();
//        for (byte b : instanceIdBytes) {
//            instanceId.append(String.format("%02X", b));
//        }
//        return instanceId.toString();
//    }

    /**
     * Updates the list of beacons on the UI.
     */
    public void updateList(Context context) {
        ListView beaconWindow = MainActivity.beacons;

        if (beaconWindow == null) {
            return;
        }

        // Sort beacons by last seen time, most recent first
        //beaconsDiscovered.sort(Comparator.comparingLong(BeaconReachable::getTimeLastFound).reversed());

        ArrayList<String> displayList = new ArrayList<>();
        for (BeaconReachable beacon : BeaconFinder.getInstance(context).getBeaconMap().values()) {
            String distance = "Distance: " + calculateDistance(beacon.getRssi());
            long lastSeen = System.currentTimeMillis() - beacon.getTimeLastFound();

            if (lastSeen > 3 * 60_000) {
                distance = "Not reachable since " + getHumanReadableTime(beacon.getTimeLastFound());
            }

            String text = beacon.getDeviceId().substring(0, 6)
                    + " ("
                    + beacon.getMacAddress()
                    + ")"
                    + "\n"
                    + distance;

            displayList.add(text);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                beaconWindow.getContext(),
                android.R.layout.simple_list_item_1,
                displayList
        );
        beaconWindow.setAdapter(adapter);

        // Add click listener to items
        beaconWindow.setOnItemClickListener((parent, view, position, id) -> {
            String selectedBeaconDetails = displayList.get(position);
            String deviceId = selectedBeaconDetails.substring(0, 6);
            BeaconReachable beacon = null;

            Collection<BeaconReachable> beacons = BeaconFinder.getInstance(context).getBeaconMap().values();
            for (BeaconReachable b : beacons) {
                if (b.getDeviceId().startsWith(deviceId)) {
                    beacon = b;
                    break;
                }
            }
            // cannot be null
            if (beacon == null) {
                Log.e(TAG, "Beacon not found: " + deviceId);
                return;
            }

            MainActivity.activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main, BeaconDetailsFragment.newInstance(beacon))
                    .addToBackStack(null)
                    .commit();
        });
    }


}
