package offgrid.geogram.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;

import offgrid.geogram.core.Log;

public class BeaconFinder {

    private static final String TAG = "BeaconFinder";

    // Eddystone Service UUID
    private static final String EDDYSTONE_SERVICE_UUID = "0000FEAA-0000-1000-8000-00805F9B34FB";

    // list of beacons that we are finding
    BeaconList beaconList = new BeaconList();

    private final Context context;
    private BluetoothAdapter bluetoothAdapter;
    private android.bluetooth.le.BluetoothLeScanner scanner;

    public BeaconFinder(Context context) {
        this.context = context;
        initializeBluetooth();
    }

    private void initializeBluetooth() {
        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "Bluetooth Manager is not available.");
            return;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled or not supported on this device.");
            return;
        }

        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            Log.e(TAG, "BLE scanning is not supported on this device.");
        } else {
            Log.i(TAG, "Bluetooth LE Scanner initialized successfully.");
        }
    }

    public void startScanning() {
        if (scanner == null) {
            Log.e(TAG, "Cannot start scanning. BLE scanner is not initialized.");
            return;
        }

        if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing BLUETOOTH_SCAN permission. Cannot start scanning.");
            return;
        }

        // Set up a filter for the Eddystone UUID
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(EDDYSTONE_SERVICE_UUID))
                .build();
        filters.add(filter);

        // Set up scan settings
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        try {
            // Start scanning
            scanner.startScan(filters, settings, scanCallback);
            Log.i(TAG, "Scanning for Eddystone beacons started.");
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while starting scanning: ");
        }
    }

    public void stopScanning() {
        if (scanner == null) {
            Log.e(TAG, "Cannot stop scanning. BLE scanner is not initialized.");
            return;
        }

        if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing BLUETOOTH_SCAN permission. Cannot stop scanning.");
            return;
        }

        try {
            scanner.stopScan(scanCallback);
            Log.i(TAG, "Scanning for Eddystone beacons stopped.");
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while stopping scanning: ");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            // Log the beacon's details
            beaconList.processBeacon(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "Scanning failed with error code: " + errorCode);
        }
    };

    private void processBeacon(ScanResult result) {
           String deviceAddress = result.getDevice().getAddress();
        int rssi = result.getRssi();
        byte[] serviceData = result.getScanRecord().getServiceData(ParcelUuid.fromString(EDDYSTONE_SERVICE_UUID));

        Log.i(TAG, "Beacon found: " + deviceAddress + ", RSSI: " + rssi);
        if (serviceData != null) {
            Log.i(TAG, "Service Data: " + bytesToHex(serviceData));
        }
    }

    // Helper method to convert bytes to a hex string
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
