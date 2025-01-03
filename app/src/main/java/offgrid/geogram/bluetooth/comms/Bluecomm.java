package offgrid.geogram.bluetooth.comms;

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
import java.util.concurrent.CopyOnWriteArrayList;

import offgrid.geogram.bluetooth.BluetoothCentral;
import offgrid.geogram.core.Log;

public class Bluecomm {

    private static final String TAG = "Bluecomm";

    private static Bluecomm instance;

    private final Context context;
    private BluetoothGatt bluetoothGatt;

    // UUIDs for service and characteristic
    private static final UUID SERVICE_UUID = BluetoothCentral.CUSTOM_SERVICE_UUID;
    private static final UUID CHARACTERISTIC_UUID = BluetoothCentral.CUSTOM_CHARACTERISTIC_UUID;

    public static final int
            timeBetweenChecks = 1000,
            timeBetweenMessages = 1000,
            maxSizeOfMessages = 14
    ;


    private Bluecomm(Context context) {
        this.context = context.getApplicationContext();
        if(cleanupThread == null){
            startCleanupThread();
        }
    }

    private final CopyOnWriteArrayList<BlueQueueItem> queue = new CopyOnWriteArrayList<>();
    private Thread cleanupThread = null;


    private void startCleanupThread() {
        cleanupThread = new Thread(() -> {
            try {
                while(true) {
                    Thread.sleep(timeBetweenChecks); // Pause for a bit
                    if(queue.isEmpty()){
                        continue;
                    }
                    while(!queue.isEmpty()){
                        BlueQueueItem item = queue.get(0);
                        writeData(item);
                        Thread.sleep(timeBetweenMessages);
                        queue.remove(0);
                    }

                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Thread interrupted: " + e.getMessage());
            }

        });
        cleanupThread.start(); // Starts the thread
    }

    /**
     * Singleton access to the GetProfile instance.
     */
    public static synchronized Bluecomm getInstance(Context context) {
        if (instance == null) {
            instance = new Bluecomm(context);
        }
        return instance;
    }

    /**
     * Reads data from the custom characteristic on a specified device.
     *
     * @param macAddress The MAC address of the device to connect to.
     */
    public void getDataRead(String macAddress, DataCallbackTemplate callback) {
        if (!checkPermissions()) {
            Log.i(TAG, "Missing required permissions to perform Bluetooth operations.");
            callback.onDataError("Missing required permissions.");
            return;
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth is not available or enabled.");
            callback.onDataError("Bluetooth is not available or enabled.");
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        if (device == null) {
            Log.i(TAG, "Device not found with MAC address: " + macAddress);
            callback.onDataError("Device not found.");
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
                        //  callback.onDataReadError("Disconnected from device.");
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
                            callback.onDataError("Service not found.");
                            gatt.disconnect();
                            return;
                        }

                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                        if (characteristic == null) {
                            Log.i(TAG, "Characteristic not found: " + CHARACTERISTIC_UUID);
                            callback.onDataError("Characteristic not found.");
                            gatt.disconnect();
                            return;
                        }

                        gatt.readCharacteristic(characteristic);
                    } else {
                        Log.i(TAG, "Failed to discover services. Status: " + status);
                        callback.onDataError("Failed to discover services.");
                        gatt.disconnect();
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        String data = new String(characteristic.getValue());
                        Log.i(TAG, "Characteristic read successfully: " + data);
                        callback.onDataSuccess(data);
                    } else {
                        Log.i(TAG, "Failed to read characteristic. Status: " + status);
                        callback.onDataError("Failed to read characteristic.");
                    }
                    gatt.disconnect();
                }
            });
        } catch (SecurityException e) {
            Log.i(TAG, "SecurityException while connecting to device: " + e.getMessage());
            callback.onDataError("Security exception occurred.");
        }
    }


    /**
     * Just send a write event to a device without waiting for the reply
     * This is useful for cases like broadcasting messages to devices
     * @param macAddress address of the device within reach
     * @param data text to be sent, attention to keep it short
     */
    public synchronized void writeData(String macAddress, String data) {
        BlueQueueItem item = new BlueQueueItem(macAddress, data);
        queue.add(item);
    }

    /**
     * Just send a write event to a device without waiting for the reply
     * This is useful for cases like broadcasting messages to devices
     */
    public synchronized void writeData(BlueQueueItem item) {
        // send data with just logging and no further reaction
        writeData(item.getMacAddress(), item.getData(), new DataCallbackTemplate() {
            @Override
            public void onDataSuccess(String data) {
                Log.i(TAG, "Data sent: " + data);
            }
            @Override
            public void onDataError(String errorMessage) {
                Log.e(TAG, "Error sending data: " + errorMessage);
            }
        });
    }

    /**
     * Writes data to the custom characteristic on a specified device without waiting for a response.
     *
     * @param macAddress The MAC address of the device to connect to.
     * @param data       The data to write.
     * @param callback   Callback to handle success or failure of the write operation.
     */
    public synchronized void writeData(String macAddress, String data, DataCallbackTemplate callback) {
        if (!checkPermissions()) {
            Log.i(TAG, "Missing required permissions to perform Bluetooth operations.");
            callback.onDataError("Missing required permissions.");
            return;
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth is not available or enabled.");
            callback.onDataError("Bluetooth is not available or enabled.");
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        if (device == null) {
            Log.i(TAG, "Device not found with MAC address: " + macAddress);
            callback.onDataError("Device not found.");
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
                            callback.onDataError("Service not found.");
                            gatt.disconnect();
                            return;
                        }

                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                        if (characteristic == null) {
                            Log.i(TAG, "Characteristic not found: " + CHARACTERISTIC_UUID);
                            callback.onDataError("Characteristic not found.");
                            gatt.disconnect();
                            return;
                        }

                        characteristic.setValue(data);
                        boolean success = gatt.writeCharacteristic(characteristic);
                        if (success) {
                            Log.i(TAG, "Write operation initiated successfully.");
                            callback.onDataSuccess("Write operation initiated.");
                        } else {
                            Log.i(TAG, "Failed to initiate write operation.");
                            callback.onDataError("Failed to initiate write operation.");
                            gatt.disconnect();
                        }
                    } else {
                        Log.i(TAG, "Failed to discover services. Status: " + status);
                        callback.onDataError("Failed to discover services.");
                        gatt.disconnect();
                    }
                }
            });
        } catch (SecurityException e) {
            Log.i(TAG, "SecurityException while connecting to device: " + e.getMessage());
            callback.onDataError("Security exception occurred.");
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


}
