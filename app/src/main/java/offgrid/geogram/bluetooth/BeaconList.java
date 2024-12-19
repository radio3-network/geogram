package offgrid.geogram.bluetooth;

import static offgrid.geogram.MainActivity.*;
import static offgrid.geogram.bluetooth.BeaconDefinitions.EDDYSTONE_SERVICE_UUID;
import static offgrid.geogram.bluetooth.BeaconFinder.bytesToHex;

import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import offgrid.geogram.MainActivity;
import offgrid.geogram.core.Log;

/**
 * Manages the list of beacons that were found
 */
public class BeaconList {

    private ArrayList<Beacon> beacons = new ArrayList<>();
    private static final String TAG = "BeaconList";

    public void processBeacon(ScanResult result) {
        boolean canUpdateList = false;
        // get basic data from the beacon
        String deviceAddress = result.getDevice().getAddress();
        int rssi = result.getRssi();

        // try to find an existing beacon of this kind
        Beacon beacon = null;
        for (Beacon b : beacons) {
            if (b.getMacAddress().equals(deviceAddress)) {
                beacon = b;
                break;
            }
        }
        // we never saw it before, create a new one
        if (beacon == null) {
            beacon = new Beacon();
            beacon.setMacAddress(deviceAddress);
            beacons.add(beacon);
            Log.i(TAG, "Beacon found: " + deviceAddress + ", RSSI: " + rssi);
            canUpdateList = true;
        } else {
            // update the timestamp of last seen
            beacon.setTimeLastFound(System.currentTimeMillis());
        }

        // update the RSSI value
        if (rssi != beacon.getRssi()) {
            beacon.setRssi(rssi);
            canUpdateList = true;
        }

        // update the service data
        byte[] serviceData = Objects.requireNonNull(result.getScanRecord())
                .getServiceData(ParcelUuid.fromString(EDDYSTONE_SERVICE_UUID));
        // only update the service data if it changed
        if (serviceData != null && Arrays.hashCode(serviceData) != Arrays.hashCode(beacon.getServiceData())) {
            beacon.setServiceData(serviceData);
            Log.i(TAG, "Service Data: " + bytesToHex(serviceData));
            canUpdateList = true;
        }

        // can we update the GUI showing the beacons?
        if (canUpdateList) {
            updateList();
        }
    }

    /**
     * Update the list of beacons on the UI
     */
    private void updateList() {
        ListView beaconWindow = MainActivity.beacons;

        if (beaconWindow == null) {
            return;
        }

        // Sort beacons by last seen time, most recent first
        Collections.sort(beacons, new Comparator<Beacon>() {
            @Override
            public int compare(Beacon b1, Beacon b2) {
                return Long.compare(b2.getTimeLastFound(), b1.getTimeLastFound());
            }
        });

        ArrayList<String> displayList = new ArrayList<>();
        for (Beacon beacon : beacons) {
            String humanReadableTime = getHumanReadableTime(beacon.getTimeLastFound());
            String displayText = beacon.getMacAddress() +
                    " | Distance: " + calculateDistance(beacon.getRssi()) +
                    (humanReadableTime.isEmpty() ? "" : " | Last Seen: " + humanReadableTime);
            displayList.add(displayText);
        }

        beaconWindow.post(() -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    beaconWindow.getContext(),
                    android.R.layout.simple_list_item_1,
                    displayList
            );
            beaconWindow.setAdapter(adapter);
        });
    }

    /**
     * Convert RSSI to a human-readable distance.
     */
    private String calculateDistance(int rssi) {
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

    /**
     * Convert the last seen timestamp into a human-readable format.
     */
    private String getHumanReadableTime(long lastSeenTimestamp) {
        long currentTime = System.currentTimeMillis();
        long elapsedMillis = currentTime - lastSeenTimestamp;

        long seconds = elapsedMillis / 1000;
        if (seconds == 0) {
            return ""; // Do not display 0 seconds ago or "Last Seen:" text
        }
        if (seconds < 60) {
            return seconds + " seconds ago";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " minutes ago";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " hours ago";
        }

        long days = hours / 24;
        return days + " days ago";
    }
}
