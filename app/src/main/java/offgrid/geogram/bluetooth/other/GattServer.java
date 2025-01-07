package offgrid.geogram.bluetooth.other;

import static offgrid.geogram.bluetooth.other.BluetoothCentral.CUSTOM_CHARACTERISTIC_UUID;
import static offgrid.geogram.bluetooth.other.BluetoothCentral.CUSTOM_SERVICE_UUID;

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
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import offgrid.geogram.bluetooth.BlueReceiver;
import offgrid.geogram.core.Log;

public class GattServer {

    private static final String TAG = "GattServer";
    private static GattServer instance;

    private final Context context;
    private BluetoothGattServer gattServer = null;
    private final Handler handler = new Handler();


    private GattServer(Context context) {
        this.context = context.getApplicationContext();
        initializeGattServer();
        startPeriodicCheck();
    }

    /**
     * Singleton access to the AppBluetoothGattServer instance.
     */
    public static synchronized GattServer getInstance(Context context) {
        if (instance == null) {
            instance = new GattServer(context);
        }
        return instance;
    }

    private final Runnable gattServerCheck = new Runnable() {
        @Override
        public void run() {
            if (gattServer == null || getConnectedDevices().isEmpty()) {
                Log.i(TAG, "Restarting GATT server due to inactivity.");
                restartGattServer();
            }
            handler.postDelayed(this, 600000); // Check every 10 minutes
        }
    };

    // Start periodic check
    public void startPeriodicCheck() {
        handler.postDelayed(gattServerCheck, 600000); // Initial delay of 10 minutes
    }

    // Stop periodic check
    public void stop() {
        handler.removeCallbacks(gattServerCheck);
        if (gattServer != null) {
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gattServer.close();
            }
            gattServer = null;
        }
    }

    public void cleanupStaleConnections() {
        List<BluetoothDevice> connectedDevices = getConnectedDevices();
        for (BluetoothDevice device : connectedDevices) {
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                try {
                    gattServer.cancelConnection(device);
                    Log.i(TAG, "Stale connection removed: " + device.getAddress());
                } catch (Exception e) {
                    Log.e(TAG, "Error while removing stale connection: " + e.getMessage());
                }
            }
        }
    }

    public void restartGattServer() {
        if (gattServer != null) {
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                try {
                    gattServer.close();
                    Log.i(TAG, "GATT server closed for restart.");
                } catch (Exception e) {
                    Log.e(TAG, "Error while closing GATT server: " + e.getMessage());
                }
            } else {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission, cannot close GATT server.");
            }
            gattServer = null;
        }
        initializeGattServer();
        Log.i(TAG, "GATT server restarted.");
    }



    /**
     * Initializes the GATT server.
     */
    private void initializeGattServer() {
        if (gattServer != null) {
            //Log.i(TAG, "GATT server is was already initialized.");
            return;
        }
        // proceed with initialization
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            try {
                CustomGattServerCallback gattServerCallback = new CustomGattServerCallback();
                gattServer = bluetoothManager.openGattServer(context, gattServerCallback);

                if (gattServer == null) {
                    Log.e(TAG, "Failed to open GATT server.");
                } else {
                    Log.i(TAG, "GATT server initialized.");
                    setupGattServer();
                }
            } catch (SecurityException e) {
                Log.i(TAG, "SecurityException while initializing GATT server: " + e.getMessage());
            }
        } else {
            Log.i(TAG, "Failed to get BluetoothManager.");
        }
    }

    private void setupGattServer() {
        if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission. Cannot add GATT service.");
            return;
        }

        try {
            BluetoothGattService customService = new BluetoothGattService(
                    CUSTOM_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

            BluetoothGattCharacteristic customCharacteristic = new BluetoothGattCharacteristic(
                    CUSTOM_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
            );

            customService.addCharacteristic(customCharacteristic);

            gattServer.addService(customService);
            Log.i(TAG, "GATT server setup complete. Custom service added.");
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while adding GATT service: " + e.getMessage());
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

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.i(TAG, "BluetoothManager is not available.");
            return new ArrayList<>();
        }

        try {
            return bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
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

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.i(TAG, "BluetoothManager is not available.");
            return new ArrayList<>();
        }

        List<BluetoothDevice> connectedDevices = new ArrayList<>();
        try {
            for (BluetoothDevice device : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)) {
                BluetoothGattService service = gattServer.getService(CUSTOM_SERVICE_UUID);
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

                // Cancel the connection with permission handling
                if (checkPermissions()) {
                    try {
                        if (gattServer != null) {
                            gattServer.cancelConnection(device);
                            Log.i(TAG, "Connection canceled for device: " + device.getAddress());
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "SecurityException while canceling connection: " + e.getMessage());
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error while canceling connection: " + e.getMessage());
                    }
                } else {
                    Log.i(TAG, "Missing required permissions to cancel connection.");
                }
            }
        }


        // Answer to a previous request
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            if (!CUSTOM_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                return;
            }
//            try {
//                BlueDataWriteFromOutside blueCentral = BlueDataWriteFromOutside.getInstance();
//                BluePackage request = blueCentral.getRequest(device.getAddress());
//                if (request == null) {
//                    Log.e(TAG, "Request not found for device: " + device.getAddress());
//                    return;
//                }
//                String message = request.getNextParcel();
//                Log.i(TAG, "Read request from " + device.getAddress() + ". Replying with: " + message);
//                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, message.getBytes());
//            } catch (SecurityException e) {
//                Log.e(TAG, "SecurityException while sending read response: " + e.getMessage());
//            }

        }

        // write request to this device was made
        @Override
        public void onCharacteristicWriteRequest(
                BluetoothDevice device,
                int requestId,
                BluetoothGattCharacteristic characteristic,
                boolean preparedWrite,
                boolean responseNeeded,
                int offset,
                byte[] value) {

            // Only accept valid UUID
            if (!CUSTOM_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                Log.e(TAG, "Request received for unknown characteristic: "
                        + characteristic.getUuid()
                        + " from " + device.getAddress()
                        + " with value: " + new String(value));
                return;
            }

            // Handle the write request in a new thread
            Thread thread = new Thread(() -> {
                try {
                    // Get the proper value for the request
                    String received = new String(value);
                    BlueReceiver dataWriteFromOutside = BlueReceiver.getInstance();
                    dataWriteFromOutside.receivingDataFromDevice(device.getAddress(), received, context);

                    // If response is needed, send it on the main thread
                    if (responseNeeded) {
                        handler.post(() -> {
                            try {
                                if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                    String responseMessage = "Acknowledged";
                                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, responseMessage.getBytes());
                                    Log.i(TAG, "Response sent to " + device.getAddress() + ": " + responseMessage);
                                } else {
                                    Log.e(TAG, "Missing BLUETOOTH_CONNECT permission, cannot send response.");
                                }
                            } catch (SecurityException e) {
                                Log.e(TAG, "SecurityException while sending write response: " + e.getMessage());
                            } catch (Exception e) {
                                Log.e(TAG, "Unexpected error while sending write response: " + e.getMessage());
                            }
                        });
                    }

                    // Optionally notify the client if notifications are enabled
                    if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        handler.post(() -> {
                            try {
                                if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                    characteristic.setValue("Notification: " + received);
                                    gattServer.notifyCharacteristicChanged(device, characteristic, false);
                                    Log.i(TAG, "Notification sent to " + device.getAddress() + ": " + received);
                                } else {
                                    Log.e(TAG, "Missing BLUETOOTH_CONNECT permission, cannot send notification.");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error while sending notification: " + e.getMessage());
                            }
                        });
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Exception while handling request: " + e.getMessage());
                }
            });
            thread.start();
        }



    }

}