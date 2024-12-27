package offgrid.geogram.bluetooth;

import static offgrid.geogram.bluetooth.BluetoothCentral.CUSTOM_CHARACTERISTIC_UUID;
import static offgrid.geogram.bluetooth.old.GenerateMessage.generateShareSSID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import offgrid.geogram.core.Log;

public class AppBluetoothGattServer {

    private static final String TAG = "AppBluetoothGattServer";
    private static AppBluetoothGattServer instance;

    private final Context context;
    private BluetoothGattServer gattServer;

    private AppBluetoothGattServer(Context context) {
        this.context = context.getApplicationContext();
        initializeGattServer();
    }

    /**
     * Singleton access to the AppBluetoothGattServer instance.
     */
    public static synchronized AppBluetoothGattServer getInstance(Context context) {
        if (instance == null) {
            instance = new AppBluetoothGattServer(context);
        }
        return instance;
    }

    /**
     * Initializes the GATT server.
     */
    private void initializeGattServer() {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            try {
                gattServer = bluetoothManager.openGattServer(context, new CustomGattServerCallback());
                Log.i(TAG, "GATT server initialized.");
            } catch (SecurityException e) {
                Log.i(TAG, "SecurityException while initializing GATT server: " + e.getMessage());
            }
        } else {
            Log.i(TAG, "Failed to get BluetoothManager.");
        }
    }

    /**
     * Retrieves a GATT characteristic by service and characteristic UUIDs.
     */
    public BluetoothGattCharacteristic getCharacteristic(UUID serviceUuid, UUID characteristicUuid) {
        if (!checkPermissions()) {
            Log.i(TAG, "Missing required permissions to access GATT services.");
            return null;
        }

        if (gattServer == null) {
            Log.i(TAG, "GATT server is not initialized.");
            return null;
        }

        try {
            BluetoothGattService service = gattServer.getService(serviceUuid);
            if (service == null) {
                Log.i(TAG, "Service not found: " + serviceUuid);
                return null;
            }

            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
            if (characteristic == null) {
                Log.i(TAG, "Characteristic not found: " + characteristicUuid);
            }
            return characteristic;
        } catch (SecurityException e) {
            Log.i(TAG, "SecurityException while retrieving characteristic: " + e.getMessage());
        }
        return null;
    }

    /**
     * Writes data to a characteristic for a specific device.
     */
    public boolean writeCharacteristic(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
        if (!checkPermissions()) {
            Log.i(TAG, "Missing required permissions to write characteristic.");
            return false;
        }

        if (gattServer == null) {
            Log.i(TAG, "GATT server is not initialized.");
            return false;
        }

        try {
            gattServer.notifyCharacteristicChanged(device, characteristic, false);
            Log.i(TAG, "Characteristic written successfully.");
            return true;
        } catch (SecurityException e) {
            Log.i(TAG, "SecurityException while writing characteristic: " + e.getMessage());
        } catch (Exception e) {
            Log.i(TAG, "Unexpected error while writing characteristic: " + e.getMessage());
        }
        return false;
    }

    /**
     * Reads data from a characteristic for a specific device.
     */
    public String readCharacteristic(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
        if (!checkPermissions()) {
            Log.i(TAG, "Missing required permissions to read characteristic.");
            return null;
        }

        if (gattServer == null) {
            Log.i(TAG, "GATT server is not initialized.");
            return null;
        }

        try {
            byte[] value = characteristic.getValue();
            if (value != null) {
                String data = new String(value);
                Log.i(TAG, "Characteristic read successfully: " + data);
                return data;
            }
        } catch (SecurityException e) {
            Log.i(TAG, "SecurityException while reading characteristic: " + e.getMessage());
        } catch (Exception e) {
            Log.i(TAG, "Unexpected error while reading characteristic: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves a list of currently connected devices.
     */
    public List<BluetoothDevice> getConnectedDevices() {
        if (!checkPermissions()) {
            Log.i(TAG, "Missing required permissions to retrieve connected devices.");
            return new ArrayList<>();
        }

        if (gattServer == null) {
            Log.i(TAG, "GATT server is not initialized.");
            return new ArrayList<>();
        }

        try {
            return gattServer.getConnectedDevices();
        } catch (SecurityException e) {
            Log.i(TAG, "SecurityException while getting connected devices: " + e.getMessage());
        } catch (Exception e) {
            Log.i(TAG, "Unexpected error while getting connected devices: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public List<BluetoothDevice> getConnectedEddystoneDevices() {
        if (!checkPermissions()) {
            Log.i(TAG, "Missing required permissions to retrieve connected devices.");
            return new ArrayList<>();
        }

        if (gattServer == null) {
            Log.i(TAG, "GATT server is not initialized.");
            return new ArrayList<>();
        }

        List<BluetoothDevice> connectedDevices = new ArrayList<>();
        try {
            for (BluetoothDevice device : gattServer.getConnectedDevices()) {
                // Check if the device has the Eddystone service
                BluetoothGattService service = gattServer.getService(BluetoothCentral.CUSTOM_SERVICE_UUID);
                if (service != null) {
                    connectedDevices.add(device);
                    Log.i(TAG, "Eddystone device found: " + device.getAddress());
                }
            }
        } catch (SecurityException e) {
            Log.i(TAG, "SecurityException while getting connected devices: " + e.getMessage());
        } catch (Exception e) {
            Log.i(TAG, "Unexpected error while getting connected devices: " + e.getMessage());
        }
        return connectedDevices;
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

    /**
     * Custom callback for GATT server operations.
     */
    private class CustomGattServerCallback extends BluetoothGattServerCallback {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Device connected: " + device.getAddress());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Device disconnected: " + device.getAddress());
            }
        }

        // Additional callbacks for characteristic read/write operations can be added here
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            if (CUSTOM_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                String message = generateShareSSID();
                try {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, message.getBytes());
                    Log.i(TAG, "Read request from " + device.getAddress());
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException while sending read response: " + e.getMessage());
                }
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (CUSTOM_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                String received = new String(value);
                Log.i(TAG, "Write request from " + device.getAddress() + ": " + received);
                if (responseNeeded) {
                    try {
                        String message = "Thanks";
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, message.getBytes());
                    } catch (SecurityException e) {
                        Log.e(TAG, "SecurityException while sending write response: " + e.getMessage());
                    }
                }
            }
        }


    }
}
