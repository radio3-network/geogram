package offgrid.geogram.bluetooth;

import static offgrid.geogram.util.BluetoothUtils.refreshDeviceCache;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.UUID;

import offgrid.geogram.bluetooth.other.comms.BlueQueueParcel;
import offgrid.geogram.bluetooth.other.comms.DataCallbackTemplate;
import offgrid.geogram.bluetooth.other.comms.Mutex;
import offgrid.geogram.core.Log;

public class Bluecomm {

    private static final String TAG = "Bluecomm";

    private static Bluecomm instance;

    private final Context context;
    private BluetoothGatt bluetoothGatt;

    // UUIDs for service and characteristic
    private static final UUID SERVICE_UUID = BluetoothCentral.UUID_SERVICE_WALKIETALKIE;
    private static final UUID CHARACTERISTIC_UUID = BluetoothCentral.UUID_CHARACTERISTIC_GENERAL;

    public static final int
            timeBetweenChecks = 1000,
            timeBetweenMessages = 1500,
            maxSizeOfMessages = 14,
            packageTimeToBeActive = 3000
    ;


    private Bluecomm(Context context) {
        this.context = context.getApplicationContext();
        // start the queues when not started already
        BlueQueueReceiving.getInstance(context).start();
        BlueQueueSending.getInstance(context).start();
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
    public synchronized void getDataRead(String macAddress, DataCallbackTemplate callback) {
        // don't send data until we are unlocked
        Mutex.getInstance().waitUntilUnlocked();

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
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Mutex.getInstance().lock();
                            try {
                                gatt.discoverServices();
                            } catch (Exception e) {
                                Log.e(TAG, "Exception during service discovery: " + e.getMessage());
                            }
                            Mutex.getInstance().unlock();
                        }, 1000); // Delay 1 second
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
                            // force to clean up the cache
                            //refreshDeviceCache(gatt);
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
            }, BluetoothDevice.TRANSPORT_LE
            );
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
        if(data == null){
            return;
        }
        // avoid sending duplicates
        if(BlueQueueSending.getInstance(context).isAlreadyOnQueueToSend(data, macAddress)){
            return;
        }
        BlueQueueParcel item = new BlueQueueParcel(macAddress, data);
        BlueQueueSending.getInstance(context).addQueueToSend(item);
    }

    /**
     * Just send a write event to a device without waiting for the reply
     * This is useful for cases like broadcasting messages to devices
     */
    public synchronized void writeData(BlueQueueParcel item) {
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
        if(data == null){
            Log.e(TAG, "Null data received for write operation to "  + macAddress);
            return;
        }
        // wait a bit until unlocked
        Mutex.getInstance().waitUntilUnlocked();

        if (checkPermissions() == false) {
            Log.i(TAG, "Missing required permissions to perform Bluetooth operations.");
            callback.onDataError("Missing required permissions.");
            return;
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            String message = "Bluetooth is not available or enabled";
            Log.i(TAG, message);
            callback.onDataError(message);
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
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Mutex.getInstance().lock();
                            try {
                                gatt.discoverServices();
                            } catch (Exception e) {
                                Log.e(TAG, "Exception during service discovery: " + e.getMessage());
                            }
                            Mutex.getInstance().unlock();
                        }, 1000); // Delay 1 second
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
                            //refreshDeviceCache(gatt);
                            return;
                        }

                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                        if (characteristic == null) {
                            Log.i(TAG, "Characteristic not found: " + CHARACTERISTIC_UUID);
                            callback.onDataError("Characteristic not found.");
                            gatt.disconnect();
                            return;
                        }

                        // Check if the characteristic supports write
                        int properties = characteristic.getProperties();
                        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0 &&
                                (properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
                            Log.e(TAG, "Characteristic does not support write operations.");
                            callback.onDataError("Characteristic does not support write operations.");
                            gatt.disconnect();
                            return;
                        }

                        // Set the write type to default
                        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

                        // Introduce a delay to ensure smooth operation
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    int result = gatt.writeCharacteristic(characteristic, data.getBytes(), BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                                    //experiment
                                    //gatt.disconnect();
                                    if (result != BluetoothGatt.GATT_SUCCESS) {
                                        Log.e(TAG, "GATT write failed with status: " + result);
                                        callback.onDataError("GATT write failed with status: " + result);
                                    } else {
                                        Log.i(TAG, "Write operation initiated successfully.");
                                        callback.onDataSuccess("Write operation initiated.");
                                    }
                                } else {
                                    // For older APIs, use the legacy method
                                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                                    boolean success = gatt.writeCharacteristic(characteristic);
                                    if (success == false) {
                                        Log.i(TAG, "Failed to initiate write operation.");
                                        callback.onDataError("Failed to initiate write operation.");
                                        gatt.disconnect();
                                    } else {
                                        Log.i(TAG, "Write operation initiated successfully.");
                                        callback.onDataSuccess("Write operation initiated.");
                                    }
                                    //experiment
                                    gatt.disconnect();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Exception during write operation: " + e.getMessage());
                                callback.onDataError("Exception during write operation: " + e.getMessage());
                                //experiment
                                gatt.disconnect();
                            }
                        }, 300); // Delay of 200ms to avoid back-to-back operations
                    } else {
                        Log.i(TAG, "Failed to discover services. Status: " + status);
                        callback.onDataError("Failed to discover services.");
                        gatt.disconnect();
                    }
                }


            }, BluetoothDevice.TRANSPORT_LE
            );
        } catch (SecurityException e) {
            Log.i(TAG, "SecurityException while connecting to device: " + e.getMessage());
            callback.onDataError("Security exception occurred.");
            //Mutex.getInstance().unlock();

        }
        //Mutex.getInstance().unlock();

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
