package offgrid.geogram.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.os.ParcelUuid;

import java.util.List;
import java.util.UUID;

import offgrid.geogram.bluetooth.old.BluetoothStateReceiver;
import offgrid.geogram.core.Log;

public class BluetoothCentral {

    private static final String TAG = "BluetoothCentral";

    private static BluetoothCentral instance;

    private final Context context;
    private GattServer gattServer;
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
        gattServer = GattServer.getInstance(context);
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

        if (gattServer == null) {
            Log.i(TAG,"Starting GATT server.");
            //gattServer = AppBluetoothGattServer.getInstance(context);
            gattServer = GattServer.getInstance(context);
        }
        // always start the gatt server
        //gattServer = GattServer.getInstance(context);

        if (beacon != null && !beacon.isAdvertising()) {
            beacon.startBeacon();
            Log.i(TAG, "Beacon started.");
        }

        if (!isScanning){
            startScanning();
        }
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

    /**
     * Checks if Bluetooth is available and enabled on the device.
     *
     * @return true if Bluetooth is available and enabled, false otherwise.
     */
    public boolean isBluetoothAvailable() {
        if (bluetoothAdapter == null) {
            Log.i(TAG, "Bluetooth is not supported on this device.");
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth is not enabled.");
            return false;
        }

        return true;
    }

}
