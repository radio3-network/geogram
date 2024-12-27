package offgrid.geogram.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.IntentFilter;
import android.os.ParcelUuid;

import java.util.List;
import java.util.UUID;

import offgrid.geogram.core.Log;

public class BluetoothCentral {

    private static final String TAG = "BluetoothCentral";

    private static BluetoothCentral instance;

    private final Context context;
    private AppBluetoothGattServer gattServer;
    private EddyBeaconAdvertise beacon;
    private BluetoothAdapter bluetoothAdapter;
    private boolean isReceiverRegistered = false;

    // Scanning state
    private boolean isScanning = false;

    // Public static UUIDs for external access
    // Eddystone Service UUID
    public static String EDDYSTONE_SERVICE_ID = "0000FEAA-0000-1000-8000-00805F9B34FB";
    public static final ParcelUuid EDDYSTONE_SERVICE_UUID = ParcelUuid.fromString(EDDYSTONE_SERVICE_ID);
    public static final UUID CUSTOM_SERVICE_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0");
    public static final UUID CUSTOM_CHARACTERISTIC_UUID = UUID.fromString("abcdef12-3456-7890-abcd-ef1234567890");

    private BluetoothCentral(Context context) {
        this.context = context.getApplicationContext();
        initialize();
    }

    /**
     * Singleton access to the BluetoothCentral instance.
     */
    public static synchronized BluetoothCentral getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothCentral(context);
        }
        return instance;
    }

    /**
     * Initializes the GATT server, beacon, and Bluetooth adapter.
     */
    private void initialize() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        gattServer = AppBluetoothGattServer.getInstance(context);
        beacon = EddyBeaconAdvertise.getInstance(context);

        registerBluetoothStateReceiver();

        Log.i(TAG, "BluetoothCentral initialized with GATT server and beacon.");
    }

    /**
     * Registers the BluetoothStateReceiver.
     */
    private void registerBluetoothStateReceiver() {
        if (!isReceiverRegistered) {
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            BluetoothStateReceiver receiver = BluetoothStateReceiver.getInstance();
            context.registerReceiver(receiver, filter);
            isReceiverRegistered = true;
            Log.i(TAG, "BluetoothStateReceiver registered.");
        } else {
            Log.i(TAG, "BluetoothStateReceiver is already registered.");
        }
    }

    /**
     * Starts the GATT server and beacon if Bluetooth is enabled.
     */
    public void start() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth is disabled. Services will not start.");
            return;
        }

        if (gattServer != null) {
            Log.i(TAG, "Starting GATT server.");
        }

        if (beacon != null) {
            beacon.startBeacon();
            Log.i(TAG, "Beacon started.");
        }

        startScanning();
    }

    /**
     * Stops the GATT server and beacon.
     */
    public void stop() {
        stopScanning();

        if (beacon != null) {
            beacon.stopBeacon();
            Log.i(TAG, "Beacon stopped.");
        }

        if (gattServer != null) {
            Log.i(TAG, "GATT server will continue to run.");
        }
    }

    /**
     * Starts Bluetooth scanning.
     */
    private void startScanning() {
        if (isScanning) {
            Log.i(TAG, "Scanning is already active.");
            return;
        }

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            BeaconFinder beaconFinder = BeaconFinder.getInstance(context);
            beaconFinder.startScanning();
            Log.i(TAG, "Beacon scanning started from BluetoothCentral.");
            isScanning = true;
        } else {
            Log.i(TAG, "Bluetooth is not enabled. Cannot start scanning.");
        }
    }

    /**
     * Stops Bluetooth scanning.
     */
    private void stopScanning() {
        if (!isScanning) {
            Log.i(TAG, "Scanning is not active.");
            return;
        }

        BeaconFinder beaconFinder = BeaconFinder.getInstance(context);
        beaconFinder.stopScanning();
        isScanning = false;
        Log.i(TAG, "Beacon scanning stopped from BluetoothCentral.");
    }

    /**
     * Returns whether scanning is currently active.
     */
    public boolean isScanningActive() {
        return isScanning;
    }

    /**
     * Broadcasts a message to all connected devices.
     */
    public void broadcastMessageToAll(String message) {
        List<BluetoothDevice> connectedDevices = gattServer.getConnectedDevices();
        if (connectedDevices.isEmpty()) {
            Log.i(TAG, "No connected devices to broadcast the message.");
            return;
        }

        for (BluetoothDevice device : connectedDevices) {
            sendMessageToDevice(device, message);
        }
    }

    public void broadcastMessageToAllEddystoneDevices(String message) {
        List<BluetoothDevice> eddystoneDevices = gattServer.getConnectedEddystoneDevices();
        if (eddystoneDevices.isEmpty()) {
            Log.i(TAG, "No Eddystone devices to broadcast the message.");
            return;
        }

        for (BluetoothDevice device : eddystoneDevices) {
            sendMessageToDevice(device, message);
        }
    }

    /**
     * Sends a message to a specific device and receives a response.
     */
    public void sendMessageToDevice(BluetoothDevice device, String message) {
        BluetoothGattCharacteristic characteristic = gattServer.getCharacteristic(CUSTOM_SERVICE_UUID, CUSTOM_CHARACTERISTIC_UUID);

        if (characteristic == null) {
            Log.i(TAG, "Custom characteristic not found.");
            return;
        }

        boolean success = gattServer.writeCharacteristic(device, characteristic);
        if (success) {
            Log.i(TAG, "Message sent to device: " + device.getAddress());
        } else {
            Log.i(TAG, "Failed to send message to device: " + device.getAddress());
        }
    }

    /**
     * Updates the beacon's namespace and instance ID dynamically.
     */
    public void updateBeaconData(String namespace, String instanceId) {
        if (beacon == null) {
            Log.i(TAG, "Beacon is not initialized.");
            return;
        }

        beacon.setNamespace(namespace);
        beacon.setInstanceId(instanceId);
        beacon.restartBeacon();
        Log.i(TAG, "Beacon data updated: Namespace=" + namespace + ", InstanceId=" + instanceId);
    }

    /**
     * Gets the current connected devices.
     */
    public List<BluetoothDevice> getConnectedDevices() {
        return gattServer.getConnectedDevices();
    }

    /**
     * Handles Bluetooth state changes.
     */
    public void handleBluetoothStateChange(boolean isEnabled) {
        if (isEnabled) {
            Log.i(TAG, "Bluetooth enabled. Starting services...");
            start();
        } else {
            Log.i(TAG, "Bluetooth disabled. Stopping services...");
            stop();
        }
    }
}
