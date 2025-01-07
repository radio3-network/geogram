package offgrid.geogram.bluetooth.other;

import static offgrid.geogram.bluetooth.other.BluetoothCentral.EDDYSTONE_SERVICE_UUID;
import static offgrid.geogram.bluetooth.broadcast.BroadcastSender.sendProfileToEveryone;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import offgrid.geogram.core.Log;
import offgrid.geogram.devices.DeviceReachable;

public class DeviceFinder {

    private static final String TAG = "DeviceFinder";
    private static final long BEACON_TIMEOUT_MS = 30000; // 30 seconds

    private static DeviceFinder instance;

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final HashMap<String, DeviceReachable> beaconMap = new HashMap<>();
    private boolean isScanning = false;
    private long timeLastUpdated = System.currentTimeMillis();

    private DeviceFinder(Context context) {
        this.context = context.getApplicationContext();
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
    }

    /**
     * Singleton access to the BeaconFinder instance.
     */
    public static synchronized DeviceFinder getInstance(Context context) {
        if (instance == null) {
            instance = new DeviceFinder(context);
        }
        return instance;
    }

    /**
     * Starts scanning for Eddystone devices.
     */
    public void startScanning() {
        if (isScanning) {
            Log.i(TAG, "Scanning is already active.");
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth is not enabled. Cannot start scanning.");
            return;
        }

        try {
            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(EDDYSTONE_SERVICE_UUID)
                    .build();

            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            bluetoothAdapter.getBluetoothLeScanner().startScan(
                    List.of(filter),
                    settings,
                    scanCallback
            );
            isScanning = true;
            Log.i(TAG, "Started scanning for Eddystone devices.");
        } catch (SecurityException e) {
            Log.i(TAG, "SecurityException while starting scan: " + e.getMessage());
        }
    }

    /**
     * Stops scanning for devices.
     */
    public void stopScanning() {
        if (!isScanning) {
            Log.i(TAG, "Scanning is not active.");
            return;
        }

        try {
            bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
            isScanning = false;
            Log.i(TAG, "Stopped scanning for devices.");
        } catch (SecurityException e) {
            Log.i(TAG, "SecurityException while stopping scan: " + e.getMessage());
        }
    }

    /**
     * Checks if scanning is currently active.
     */
    public boolean isScanningActive() {
        return isScanning;
    }

    /**
     * Gets the up-to-date HashMap of discovered beacons.
     */
    public HashMap<String, DeviceReachable> getDeviceMap() {
        return beaconMap;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                processScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i(TAG, "Scan failed with error code: " + errorCode);
        }
    };

    /**
     * Processes a scan result and updates the beacon map.
     */
    private void processScanResult(ScanResult result) {
        if (result == null || result.getScanRecord() == null) {
            return;
        }

        // reduce stress on the CPU
        long timePassed = System.currentTimeMillis() - timeLastUpdated;
        if(timePassed > 2000){
            timeLastUpdated = System.currentTimeMillis();
        }else{
            return;
        }

        // wait for write operations to be over
//        Mutex.getInstance().waitUntilUnlocked();
//        Mutex.getInstance().lock();
        byte[] serviceData = result.getScanRecord().getServiceData(EDDYSTONE_SERVICE_UUID);
        //Mutex.getInstance().unlock();
        if (serviceData == null) {
            return;
        }

        String deviceId = extractInstanceId(serviceData);

        // for the moment we provide a full large number instead of shorter
        if(deviceId.length() == 12){
            deviceId = deviceId.substring(0, deviceId.length() - 6);
        }
        //Log.i(TAG, "Found Eddystone beacon: " + deviceId);

        String namespaceId = extractNamespaceId(serviceData);

        // is this beacon already on our hashmap?
        DeviceReachable beacon = beaconMap.get(deviceId);

        // seems like a new one
        if (beacon == null) {
            beacon = new DeviceReachable();
            beacon.setInstanceId(deviceId);
            beacon.setNamespaceId(namespaceId);
            beacon.setMacAddress(result.getDevice().getAddress());
            beacon.setRssi(result.getRssi());
            beaconMap.put(deviceId, beacon);
            // send our own profile info to all reachable devices
            // this way they can know about us.
            sendProfileToEveryone(context);

            // also save it do disk
            //BeaconDatabase.saveBeaconToDisk(beacon, context);
            Log.i(TAG, "New Eddystone beacon found: "
                    + deviceId
                    + " at "
                    + result.getDevice().getAddress()
            );

            // update the list visible on the main screen
            DeviceListing.getInstance().updateList(this.context);
            Log.i(TAG, "Updating beacon list on UI");
        }

        // setup the usual data
        beacon.setTimeLastFound(System.currentTimeMillis());
        beacon.setRssi(result.getRssi());
        beacon.setServiceData(serviceData);
        beacon.setMacAddress(result.getDevice().getAddress());
        //Log.i(TAG, "Updated beacon: " + instanceId + " RSSI: " + result.getRssi());
    }


    /**
     * Cleans up beacons that haven't been seen for a while.
     */
    public void cleanupDisconnectedDevices() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, DeviceReachable>> iterator = beaconMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, DeviceReachable> entry = iterator.next();
            DeviceReachable beacon = entry.getValue();
            if (currentTime - beacon.getTimeLastFound() > BEACON_TIMEOUT_MS) {
                Log.i(TAG, "Removing disconnected beacon: " + entry.getKey());
                iterator.remove();
            }
        }
    }

    /**
     * Extracts the Namespace ID from service data.
     */
    private String extractNamespaceId(byte[] serviceData) {
        if (serviceData.length < 12) {
            return "Unknown";
        }
        return bytesToHex(serviceData, 2, 10);
    }

    /**
     * Extracts the Instance ID from service data.
     */
    private String extractInstanceId(byte[] serviceData) {
        if (serviceData.length < 18) {
            return "Unknown";
        }
        return bytesToHex(serviceData, 12, 6);
    }

    /**
     * Converts a byte array to a hexadecimal string.
     */
    private String bytesToHex(byte[] bytes, int offset, int length) {
        StringBuilder hexString = new StringBuilder();
        for (int i = offset; i < offset + length; i++) {
            hexString.append(String.format("%02X", bytes[i]));
        }
        return hexString.toString();
    }

    /**
     * Updates a beacon.
     * @param beacon
     */
    public void update(DeviceReachable beacon) {
        beaconMap.put(beacon.getDeviceId(), beacon);
    }
}
