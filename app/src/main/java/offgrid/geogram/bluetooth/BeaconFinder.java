package offgrid.geogram.bluetooth;

import static offgrid.geogram.bluetooth.BluetoothCentral.EDDYSTONE_SERVICE_UUID;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import offgrid.geogram.core.Log;
import offgrid.geogram.database.BeaconDatabase;
import offgrid.geogram.things.BeaconReachable;

public class BeaconFinder {

    private static final String TAG = "BeaconFinder";
    private static final long BEACON_TIMEOUT_MS = 30000; // 30 seconds

    private static BeaconFinder instance;

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final HashMap<String, BeaconReachable> beaconMap = new HashMap<>();
    private boolean isScanning = false;

    private BeaconFinder(Context context) {
        this.context = context.getApplicationContext();
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
    }

    /**
     * Singleton access to the BeaconFinder instance.
     */
    public static synchronized BeaconFinder getInstance(Context context) {
        if (instance == null) {
            instance = new BeaconFinder(context);
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
    public HashMap<String, BeaconReachable> getBeaconMap() {
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

        byte[] serviceData = result.getScanRecord().getServiceData(EDDYSTONE_SERVICE_UUID);
        if (serviceData == null) {
            return;
        }

        String instanceId = extractInstanceId(serviceData);
        String namespaceId = extractNamespaceId(serviceData);

        // is this beacon already on our hashmap?
        BeaconReachable beacon = beaconMap.get(instanceId);

        // seems like a new one
        if (beacon == null) {
            beacon = new BeaconReachable();
            beacon.setInstanceId(instanceId);
            beacon.setNamespaceId(namespaceId);
            beacon.setMacAddress(result.getDevice().getAddress());
            beaconMap.put(instanceId, beacon);
            // also save it do disk
            BeaconDatabase.saveOrMergeWithBeaconDiscovered(beacon, context);
            Log.i(TAG, "New Eddystone beacon found: " + instanceId);

//            if (!checkPermissions()) {
//                Log.i(TAG, "Missing required permissions to bond");
//                return;
//            }else{
//                //boolean bonded = result.getDevice().createBond();
//            }



        }

        // setup the usual data
        beacon.setTimeLastFound(System.currentTimeMillis());
        beacon.setRssi(result.getRssi());
        beacon.setServiceData(serviceData);
        //Log.i(TAG, "Updated beacon: " + instanceId + " RSSI: " + result.getRssi());
    }

    /**
     * Cleans up beacons that haven't been seen for a while.
     */
    public void cleanupDisconnectedDevices() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, BeaconReachable>> iterator = beaconMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, BeaconReachable> entry = iterator.next();
            BeaconReachable beacon = entry.getValue();
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
     * Checks if the required permissions are granted.
     */
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31 or higher
            boolean hasConnectPermission =
                    context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED;

            if (!hasConnectPermission) {
                Log.i(TAG, "Missing BLUETOOTH_CONNECT permission.");
            }
            return hasConnectPermission;
        } else {
            // Older Android versions do not require runtime permissions for Bluetooth
            return true;
        }
    }


}
