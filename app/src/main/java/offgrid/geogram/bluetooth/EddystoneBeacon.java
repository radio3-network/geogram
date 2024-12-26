package offgrid.geogram.bluetooth;

import static offgrid.geogram.bluetooth.GenerateMessage.generateShareSSID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import offgrid.geogram.core.Log;

public class EddystoneBeacon {

    private static final String TAG = "EddystoneBeacon";

    // Eddystone Service UUID
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID =
            ParcelUuid.fromString(BeaconDefinitions.EDDYSTONE_SERVICE_UUID);

    // Custom Service and Characteristic UUIDs
    private static final UUID CUSTOM_SERVICE_UUID =
            UUID.fromString(BeaconDefinitions.CUSTOM_SERVICE_UUID);
    private static final UUID CUSTOM_CHARACTERISTIC_UUID =
            UUID.fromString(BeaconDefinitions.CUSTOM_CHARACTERISTIC_UUID);

    private final Context context;
    private BluetoothAdapter bluetoothAdapter;
    private android.bluetooth.le.BluetoothLeAdvertiser advertiser;
    private AdvertiseCallback advertiseCallback;
    private BluetoothGattServer gattServer;

    public EddystoneBeacon(Context context) {
        this.context = context;
        initializeBluetooth();
    }

    public void setDeviceName(String newName) {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter is not initialized. Cannot change device name.");
            return;
        }

        if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission. Cannot change device name.");
            return;
        }

        boolean result = bluetoothAdapter.setName(newName);
        if (result) {
            Log.i(TAG, "Device name changed to: " + newName);
        } else {
            Log.e(TAG, "Failed to change device name.");
        }
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

        // setup the name
        //setDeviceName("geogram");

        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) {
            Log.e(TAG, "BLE advertising is not supported on this device.");
        } else {
            Log.i(TAG, "Bluetooth LE Advertiser initialized successfully.");
            Log.i(TAG, "Device MAC Address: " + bluetoothAdapter.getAddress());
        }

        if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission. GATT server cannot be initialized.");
            return;
        }

        gattServer = bluetoothManager.openGattServer(context, new GattServerCallback());
        if (gattServer == null) {
            Log.e(TAG, "Failed to open GATT server.");
        } else {
            setupGattServer();
        }
    }

    private void setupGattServer() {
        if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission. Cannot add GATT service.");
            return;
        }

        BluetoothGattService customService = new BluetoothGattService(
                CUSTOM_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic customCharacteristic = new BluetoothGattCharacteristic(
                CUSTOM_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
        );

        customService.addCharacteristic(customCharacteristic);

        try {
            gattServer.addService(customService);
            Log.i(TAG, "GATT server setup complete. Custom service added.");
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while adding GATT service: ");
        }
    }


    public void startAdvertising(String namespaceId) {
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
                    .setConnectable(true)
                    .build();

            AdvertiseData advertiseData = new AdvertiseData.Builder()
                    .addServiceUuid(EDDYSTONE_SERVICE_UUID)
                    .addServiceData(EDDYSTONE_SERVICE_UUID, buildEddystoneUidFrame(namespaceId))
                    .setIncludeDeviceName(false)
                    .build();

            Log.i(TAG, "AdvertiseSettings: " + settings.toString());
            Log.i(TAG, "AdvertiseData: " + advertiseData.toString());

            advertiseCallback = new AdvertiseCallback() {
                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    Log.i(TAG, "Eddystone beacon started successfully.");
                    Log.i(TAG, "Broadcasting Eddystone UID Frame:");
                    Log.i(TAG, "Namespace ID: " + namespaceId);
                    Log.i(TAG, "Device ID: " + BeaconDefinitions.deviceId);
                }

                @Override
                public void onStartFailure(int errorCode) {
                    Log.e(TAG, "Failed to start Eddystone beacon. Error code: " + errorCode);
                }
            };

            Log.i(TAG, "Starting advertising...");
            advertiser.startAdvertising(settings, advertiseData, advertiseCallback);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while starting advertising: ");
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while starting advertising: ");
        }
    }




    public void stopAdvertising() {
        if (advertiser != null && advertiseCallback != null) {
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing BLUETOOTH_ADVERTISE permission. Cannot stop advertising.");
                return;
            }

            try {
                advertiser.stopAdvertising(advertiseCallback);
                Log.i(TAG, "Eddystone beacon stopped.");
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException while stopping advertising: ");
            }
        }

        if (gattServer != null) {
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission. Cannot close GATT server.");
                return;
            }

            gattServer.close();
            Log.i(TAG, "GATT server closed.");
        }
    }


    private boolean checkPermissions() {
        boolean hasAdvertisePermission =
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE)
                        == PackageManager.PERMISSION_GRANTED;
        boolean hasConnectPermission =
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED;

        if (!hasAdvertisePermission) {
            Log.i(TAG, "Missing BLUETOOTH_ADVERTISE permission.");
        }

        if (!hasConnectPermission) {
            Log.i(TAG, "Missing BLUETOOTH_CONNECT permission.");
        }

        return hasAdvertisePermission && hasConnectPermission;
    }

    private class GattServerCallback extends BluetoothGattServerCallback {
        @Override
        public void onConnectionStateChange(android.bluetooth.BluetoothDevice device, int status, int newState) {
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission for connection state change.");
                return;
            }

            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Device connected: " + device.getAddress());
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Device disconnected: " + device.getAddress());
            }
        }

        @Override
        public void onCharacteristicReadRequest(android.bluetooth.BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission for read request.");
                return;
            }

            if (CUSTOM_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                String message = generateShareSSID();
                gattServer.sendResponse(device, requestId, android.bluetooth.BluetoothGatt.GATT_SUCCESS, offset, message.getBytes());
                Log.i(TAG, "Read request from " + device.getAddress());
            }
        }

        @Override
        public void onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission for write request.");
                return;
            }

            if (CUSTOM_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                String received = new String(value);
                Log.i(TAG, "Write request from " + device.getAddress() + ": " + received);
                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, android.bluetooth.BluetoothGatt.GATT_SUCCESS, offset, null);
                }
            }
        }
    }
    private byte[] buildEddystoneUidFrame(String namespaceId) {
        if (namespaceId.length() != 20) {
            throw new IllegalArgumentException("Namespace ID must be 20 hex characters.");
        }

        try {
            byte[] namespaceBytes = hexStringToByteArray(namespaceId);

            // Generate a unique Instance ID
            BeaconDefinitions.deviceId = GenerateDeviceId.generateInstanceId(context);
            byte[] instanceBytes = hexStringToByteArray(BeaconDefinitions.deviceId);

            ByteBuffer buffer = ByteBuffer.allocate(20);
            buffer.put((byte) 0x00); // Frame type: UID
            buffer.put((byte) 0x00); // TX power level (placeholder)
            buffer.put(namespaceBytes); // Namespace ID (10 bytes)
            buffer.put(instanceBytes); // Instance ID (6 bytes)
            buffer.put((byte) 0x00); // Reserved byte
            buffer.put((byte) 0x00); // Reserved byte

            return buffer.array();
        } catch (Exception e) {
            Log.e(TAG, "Error building Eddystone UID Frame: " + e.getMessage());
            throw e;
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

    /**
     * Broadcasts a message to all reachable EddyStone beacon devices.
     * @param message
     */
    public boolean broadcastMessage(String message) {
        if (gattServer == null) {
            Log.e(TAG, "GATT server is not initialized.");
            return false;
        }

        if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission. Cannot broadcast message.");
            return false;
        }

        BluetoothGattService service = gattServer.getService(CUSTOM_SERVICE_UUID);
        if (service == null) {
            Log.e(TAG, "Custom service not found.");
            return false;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CUSTOM_CHARACTERISTIC_UUID);
        if (characteristic == null) {
            Log.e(TAG, "Custom characteristic not found.");
            return false;
        }

        characteristic.setValue(message.getBytes());
        Log.i(TAG, "Broadcasting message: " + message);

        // Use BluetoothManager to get connected devices
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "BluetoothManager is not available.");
            return false;
        }

        List<BluetoothDevice> connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
        Log.i(TAG, "Connected devices count: " + connectedDevices.size());

        for (BluetoothDevice device : connectedDevices) {
            try {
                boolean notificationSent = gattServer.notifyCharacteristicChanged(device, characteristic, false);
                if (notificationSent) {
                    Log.i(TAG, "Message broadcasted to device: " + device.getAddress());
                } else {
                    Log.e(TAG, "Failed to broadcast message to device: " + device.getAddress());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error broadcasting message to device: "
                        + device.getAddress() + " " + e.getMessage());
            }
        }

        return true;
    }




}