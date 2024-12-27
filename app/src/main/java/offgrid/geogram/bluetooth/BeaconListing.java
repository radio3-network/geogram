package offgrid.geogram.bluetooth;

import static offgrid.geogram.bluetooth.BluetoothCentral.EDDYSTONE_SERVICE_UUID;
import static offgrid.geogram.util.DateUtils.getHumanReadableTime;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import offgrid.geogram.MainActivity;
import offgrid.geogram.R;
import offgrid.geogram.database.BeaconDatabase;
import offgrid.geogram.things.BeaconReachable;
import offgrid.geogram.core.Log;
import offgrid.geogram.fragments.BeaconDetailsFragment;

/**
 * Manages the list of beacons that were found.
 */
public class BeaconListing {

    private static final String TAG = "BeaconList";

    private static BeaconListing instance; // Singleton instance

    // When was the window last updated?
    private long lastUpdated = System.currentTimeMillis();

    // The real list of beacons
    public static ArrayList<BeaconReachable> beaconsDiscovered = new ArrayList<>();

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

    /**
     * Processes a beacon scan result.
     */
    public void processBeacon(ScanResult result, Context context) {

        // Only update after some time
        long timeNow = System.currentTimeMillis();
        long timePassed = timeNow - lastUpdated;
        if (timePassed < 2000) {
            return;
        }
        lastUpdated = timeNow;

        boolean canUpdateList = false;

        // Get basic data from the beacon
        String deviceAddress = result.getDevice().getAddress();
        int rssi = result.getRssi();

        // Get advanced data
        String namespaceId = null;
        String instanceId = null;
        byte[] serviceData = Objects.requireNonNull(result.getScanRecord())
                .getServiceData(EDDYSTONE_SERVICE_UUID);

        // Skip if no service data
        if (serviceData == null) {
            return;
        } else {
            namespaceId = extractNamespaceId(serviceData);
            instanceId = extractInstanceId(serviceData);
        }

        // Find an existing beacon of this kind
        BeaconReachable beacon = null;

        for (BeaconReachable b : beaconsDiscovered) {
            if (b.getDeviceId().equals(instanceId)) {
                beacon = b;
                beacon.setMacAddress(deviceAddress);
                beacon.setTimeLastFound(System.currentTimeMillis());
                break;
            }
        }

        // Create a new beacon if none found
        if (beacon == null) {
            beacon = new BeaconReachable();
            beacon.setMacAddress(deviceAddress);
            beaconsDiscovered.add(beacon);
            Log.i(TAG, "Beacon found: " + deviceAddress + ", RSSI: " + rssi);
            canUpdateList = true;
        } else {
            // Update the timestamp of last seen
            beacon.setTimeLastFound(System.currentTimeMillis());
        }

        // Update the RSSI value
        if (rssi != beacon.getRssi()) {
            beacon.setRssi(rssi);
            canUpdateList = true;
        }

        if (beacon.getDeviceId() == null) {
            beacon.setNamespaceId(namespaceId);
            beacon.setInstanceId(instanceId);
            canUpdateList = true;
        }

        // Update the service data if it changed
        if (Arrays.hashCode(serviceData) != Arrays.hashCode(beacon.getServiceData())) {
            beacon.setServiceData(serviceData);
            canUpdateList = true;
        }

        // Save to disk
        BeaconDatabase.saveOrMergeWithBeaconDiscovered(beacon, context);

        // Update the GUI showing the beacons
        if (canUpdateList) {
            updateList();
        }
    }

    /**
     * Extracts the Namespace ID from service data.
     */
    private String extractNamespaceId(byte[] serviceData) {
        if (serviceData == null || serviceData.length < 18) { // Ensure valid service data length
            Log.e(TAG, "Invalid service data for Eddystone UID.");
            return "Unknown";
        }

        byte[] namespaceIdBytes = Arrays.copyOfRange(serviceData, 2, 12);
        StringBuilder namespaceId = new StringBuilder();
        for (byte b : namespaceIdBytes) {
            namespaceId.append(String.format("%02X", b));
        }
        return namespaceId.toString();
    }

    /**
     * Extracts the Instance ID from service data.
     */
    private String extractInstanceId(byte[] serviceData) {
        if (serviceData == null || serviceData.length < 18) { // Ensure valid service data length
            Log.e(TAG, "Invalid service data for Eddystone UID.");
            return "Unknown";
        }

        byte[] instanceIdBytes = Arrays.copyOfRange(serviceData, 12, 18);
        StringBuilder instanceId = new StringBuilder();
        for (byte b : instanceIdBytes) {
            instanceId.append(String.format("%02X", b));
        }
        return instanceId.toString();
    }

    /**
     * Updates the list of beacons on the UI.
     */
    public void updateList() {
        ListView beaconWindow = MainActivity.beacons;

        if (beaconWindow == null) {
            return;
        }

        // Sort beacons by last seen time, most recent first
        beaconsDiscovered.sort(Comparator.comparingLong(BeaconReachable::getTimeLastFound).reversed());

        ArrayList<String> displayList = new ArrayList<>();
        for (BeaconReachable beacon : beaconsDiscovered) {
            String distance = "Distance: " + calculateDistance(beacon.getRssi());
            long lastSeen = System.currentTimeMillis() - beacon.getTimeLastFound();

            if (lastSeen > 3 * 60_000) {
                distance = "Not reachable since " + getHumanReadableTime(beacon.getTimeLastFound());
            }

            String displayText = beacon.getDeviceId() + "\n" + distance;
            displayList.add(displayText);
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
            MainActivity.activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main, BeaconDetailsFragment.newInstance(selectedBeaconDetails, position))
                    .addToBackStack(null)
                    .commit();
        });
    }

    /**
     * Converts RSSI to a human-readable distance.
     */
    public static String calculateDistance(int rssi) {
        double txPower = -59; // Default Tx Power for BLE beacons
        if (rssi == 0) {
            return "Unknown";
        }

        double ratio = rssi * 1.0 / txPower;
        double distance;
        if (ratio < 1.0) {
            distance = Math.pow(ratio, 10);
        } else {
            distance = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }

        if (distance < 0.5) {
            return "Next to you";
        } else {
            return String.format("%.2f meters", distance);
        }
    }
}
