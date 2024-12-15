package offgrid.geogram.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import java.nio.ByteBuffer;

public class EddystoneBeacon {

    private static final String TAG = "EddystoneBeacon";

    // Eddystone Service UUID
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");

    private final Context context;
    private BluetoothAdapter bluetoothAdapter;
    private android.bluetooth.le.BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;

    public EddystoneBeacon(Context context) {
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

        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) {
            Log.e(TAG, "BLE advertising is not supported on this device.");
        } else {
            Log.i(TAG, "Bluetooth LE Advertiser initialized successfully.");
            Log.i(TAG, "Device MAC Address: " + bluetoothAdapter.getAddress());
        }
    }

    public void startAdvertising(String namespaceId, String instanceId) {
        if (advertiser == null) {
            Log.e(TAG, "Advertiser is null. Cannot start advertising.");
            return;
        }

        if (!checkPermissions()) {
            Log.e(TAG, "Bluetooth permissions are not granted.");
            return;
        }

        try {
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setConnectable(false)
                    .build();

            AdvertiseData advertiseData = new AdvertiseData.Builder()
                    .addServiceUuid(EDDYSTONE_SERVICE_UUID)
                    .addServiceData(EDDYSTONE_SERVICE_UUID,
                            buildEddystoneUidFrame(namespaceId, instanceId))
                    .setIncludeDeviceName(false)
                    .build();

            // Log detailed settings and data
            Log.i(TAG, "AdvertiseSettings: " + settings.toString());
            Log.i(TAG, "AdvertiseData: " + advertiseData.toString());

            advertiseCallback = new AdvertiseCallback() {
                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    Log.i(TAG, "Eddystone beacon started successfully.");
                    Log.i(TAG, "Broadcasting Eddystone UID Frame:");
                    Log.i(TAG, "Namespace ID: " + namespaceId);
                    Log.i(TAG, "Instance ID: " + instanceId);
                }

                @Override
                public void onStartFailure(int errorCode) {
                    Log.e(TAG, "Failed to start Eddystone beacon. Error code: " + errorCode);
                }
            };

            Log.i(TAG, "Starting advertising...");
            advertiser.startAdvertising(settings, advertiseData, advertiseCallback);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while starting advertising: ", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while starting advertising: ", e);
        }
    }

    public void stopAdvertising() {
        if (advertiser != null && advertiseCallback != null) {
            try {
                advertiser.stopAdvertising(advertiseCallback);
                Log.i(TAG, "Eddystone beacon stopped.");
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: " + e.getMessage());
            }
        }
    }

    private boolean checkPermissions() {
        if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing BLUETOOTH_ADVERTISE permission.");
            return false;
        }
        Log.i(TAG, "Bluetooth advertise permission granted.");
        return true;
    }

    private byte[] buildEddystoneUidFrame(String namespaceId, String instanceId) {
        if (namespaceId.length() != 20 || instanceId.length() != 12) {
            throw new IllegalArgumentException("Namespace ID must be 20 hex characters and Instance ID must be 12 hex characters.");
        }

        try {
            byte[] namespaceBytes = hexStringToByteArray(namespaceId);
            byte[] instanceBytes = hexStringToByteArray(instanceId);

            // Log lengths for debugging
            Log.i(TAG, "Namespace ID length: " + namespaceBytes.length + " bytes.");
            Log.i(TAG, "Instance ID length: " + instanceBytes.length + " bytes.");

            // Allocate buffer size dynamically based on required size
            ByteBuffer buffer = ByteBuffer.allocate(2 + namespaceBytes.length + instanceBytes.length + 2); // Frame type + TX power + Namespace + Instance + Reserved bytes
            buffer.put((byte) 0x00); // Frame type: UID
            buffer.put((byte) 0x00); // TX power level (placeholder)

            // Add Namespace ID and Instance ID
            buffer.put(namespaceBytes);
            buffer.put(instanceBytes);

            // Reserved bytes
            buffer.put((byte) 0x00);
            buffer.put((byte) 0x00);

            Log.i(TAG, "Eddystone UID Frame built successfully.");
            return buffer.array();
        } catch (Exception e) {
            Log.e(TAG, "Error building Eddystone UID Frame: ", e);
            throw e; // Rethrow exception for visibility
        }
    }

    private byte[] hexStringToByteArray(String hex) {
        int length = hex.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
