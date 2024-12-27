package offgrid.geogram.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.UUID;

import offgrid.geogram.core.Log;

public class GetProfile {

    private static final String TAG = "GetProfile";

    private static GetProfile instance;

    private final Context context;
    private BluetoothGatt bluetoothGatt;

    // UUIDs for service and characteristic
    private static final UUID SERVICE_UUID = BluetoothCentral.CUSTOM_SERVICE_UUID;
    private static final UUID CHARACTERISTIC_UUID = BluetoothCentral.CUSTOM_CHARACTERISTIC_UUID;

    private GetProfile(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Singleton access to the GetProfile instance.
     */
    public static synchronized GetProfile getInstance(Context context) {
        if (instance == null) {
            instance = new GetProfile(context);
        }
        return instance;
    }

    /**
     * Reads data from the custom characteristic on a specified device.
     *
     * @param macAddress The MAC address of the device to connect to.
     * @return The read data as a String, or null if an error occurs.
     */
    public void getDataRead(String macAddress, DataReadCallback callback) {
        if (!checkPermissions()) {
            Log.i(TAG, "Missing required permissions to perform Bluetooth operations.");
            callback.onDataReadError("Missing required permissions.");
            return;
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth is not available or enabled.");
            callback.onDataReadError("Bluetooth is not available or enabled.");
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        if (device == null) {
            Log.i(TAG, "Device not found with MAC address: " + macAddress);
            callback.onDataReadError("Device not found.");
            return;
        }

        try {
            bluetoothGatt = device.connectGatt(context, false, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        Log.i(TAG, "Connected to GATT server.");
                        gatt.discoverServices();
                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        Log.i(TAG, "Disconnected from GATT server.");
                        callback.onDataReadError("Disconnected from device.");
                        bluetoothGatt.close();
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        BluetoothGattService service = gatt.getService(SERVICE_UUID);
                        if (service == null) {
                            Log.i(TAG, "Service not found: " + SERVICE_UUID);
                            callback.onDataReadError("Service not found.");
                            gatt.disconnect();
                            return;
                        }

                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                        if (characteristic == null) {
                            Log.i(TAG, "Characteristic not found: " + CHARACTERISTIC_UUID);
                            callback.onDataReadError("Characteristic not found.");
                            gatt.disconnect();
                            return;
                        }

                        gatt.readCharacteristic(characteristic);
                    } else {
                        Log.i(TAG, "Failed to discover services. Status: " + status);
                        callback.onDataReadError("Failed to discover services.");
                        gatt.disconnect();
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        String data = new String(characteristic.getValue());
                        Log.i(TAG, "Characteristic read successfully: " + data);
                        callback.onDataReadSuccess(data);
                    } else {
                        Log.i(TAG, "Failed to read characteristic. Status: " + status);
                        callback.onDataReadError("Failed to read characteristic.");
                    }
                    gatt.disconnect();
                }
            });
        } catch (SecurityException e) {
            Log.i(TAG, "SecurityException while connecting to device: " + e.getMessage());
            callback.onDataReadError("Security exception occurred.");
        }
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
     * Callback interface for data read operations.
     */
    public interface DataReadCallback {
        void onDataReadSuccess(String data);

        void onDataReadError(String error);
    }
}
