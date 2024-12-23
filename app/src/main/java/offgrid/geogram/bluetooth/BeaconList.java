package offgrid.geogram.bluetooth;

import static offgrid.geogram.MainActivity.*;
import static offgrid.geogram.bluetooth.BeaconDefinitions.EDDYSTONE_SERVICE_UUID;
import static offgrid.geogram.bluetooth.BeaconDefinitions.namespaceId;
import static offgrid.geogram.bluetooth.BeaconFinder.bytesToHex;

import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import offgrid.geogram.MainActivity;
import offgrid.geogram.R;
import offgrid.geogram.core.Log;
import offgrid.geogram.fragments.BeaconDetailsFragment;

/**
 * Manages the list of beacons that were found
 */
public class BeaconList {

    // when was the window last time updated?
    private long lastUpdated = System.currentTimeMillis();

    private ArrayList<Beacon> beacons = new ArrayList<>();
    private static final String TAG = "BeaconList";

    public void processBeacon(ScanResult result) {

        // only update after some time
        long timeNow = System.currentTimeMillis();
        long timePassed = timeNow - lastUpdated;
        if(timePassed < 2000){
            return;
        }
        lastUpdated = timeNow;


        boolean canUpdateList = false;
        // get basic data from the beacon
        String deviceAddress = result.getDevice().getAddress();
        int rssi = result.getRssi();


        // get more advanced data
        // update the service data
        String namespaceId = null;
        String instanceId = null;
        byte[] serviceData = Objects.requireNonNull(result.getScanRecord())
                .getServiceData(ParcelUuid.fromString(EDDYSTONE_SERVICE_UUID));
        // can often be null
        if(serviceData != null){
            namespaceId = extractNamespaceId(serviceData);
            instanceId = extractInstanceId(serviceData);
        }else{
            // don't process empty beacons
            return;
        }

        // try to find an existing beacon of this kind
        Beacon beacon = null;
        for (Beacon b : beacons) {
            if (b.getMacAddress().equals(deviceAddress)) {
                beacon = b;
                break;
            }
        }

        // try to do this based on instanceId
        if(instanceId != null && beacon == null){
            for (Beacon b : beacons) {
                // beacon is recognized, use it
                if (b.getInstanceId().equals(instanceId)) {
                    beacon = b;
                    beacon.setMacAddress(deviceAddress);
                    break;
                }
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

        if(beacon.getInstanceId() == null && instanceId != null){
            beacon.setNamespaceId(namespaceId);
            beacon.setInstanceId(instanceId);
            canUpdateList = true;
        }


        // only update the service data if it changed
        if (Arrays.hashCode(serviceData) != Arrays.hashCode(beacon.getServiceData())) {
            beacon.setServiceData(serviceData);

            // Extract and log the instance ID
            //Log.i(TAG, "Instance ID: " + instanceId);

            canUpdateList = true;
        }

        // can we update the GUI showing the beacons?
        if (canUpdateList) {
            updateList();
        }
    }

    /**
     * Extract the Namespace ID from service data.
     */
    private String extractNamespaceId(byte[] serviceData) {
        if (serviceData == null || serviceData.length < 18) { // Ensure valid service data length
            Log.e(TAG, "Invalid service data for Eddystone UID.");
            return "Unknown";
        }

        // Extract Instance ID (6 bytes starting at offset 12)
        byte[] instanceIdBytes = Arrays.copyOfRange(serviceData, 2, 12);

        // Convert to hexadecimal string
        StringBuilder namespaceId = new StringBuilder();
        for (byte b : instanceIdBytes) {
            namespaceId.append(String.format("%02X", b));
        }
        return namespaceId.toString();
    }

    /**
     * Extract the Instance ID from service data.
     */
    private String extractInstanceId(byte[] serviceData) {
        if (serviceData == null || serviceData.length < 18) { // Ensure valid service data length
            Log.e(TAG, "Invalid service data for Eddystone UID.");
            return "Unknown";
        }

        // Extract Instance ID (6 bytes starting at offset 12)
        byte[] instanceIdBytes = Arrays.copyOfRange(serviceData, 12, 18);

        // Convert to hexadecimal string
        StringBuilder instanceId = new StringBuilder();
        for (byte b : instanceIdBytes) {
            instanceId.append(String.format("%02X", b));
        }
        return instanceId.toString();
    }

    /**
     * Update the list of beacons on the UI
     */
    public void updateList() {
        ListView beaconWindow = MainActivity.beacons;

        // remove empty label
        //activity.updateEmptyViewVisibilityBeforeUpdate();

        if (beaconWindow == null) {
            return;
        }

        // Sort beacons by last seen time, most recent first
        beacons.sort(Comparator.comparingLong(Beacon::getTimeLastFound).reversed());

        ArrayList<String> displayList = new ArrayList<>();
        for (Beacon beacon : beacons) {
            String displayText = beacon.getInstanceId() +
                    " | Distance: " + calculateDistance(beacon.getRssi());
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

            // Replace the current fragment with the details fragment
            MainActivity.activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main, BeaconDetailsFragment.newInstance(selectedBeaconDetails))
                    .addToBackStack(null)
                    .commit();
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
            if(minutes == 1){
                return minutes + " minute ago";
            }else{
                return minutes + " minutes ago";
            }
        }

        long hours = minutes / 60;
        if (hours < 24) {
            if(hours == 1){
                return hours + " hour ago";
            }else{
                return hours + " hours ago";
            }
        }

        long days = hours / 24;
        if(days == 1){
            return days + " day ago";
        }else{
            return days + " days ago";
        }
    }
}
