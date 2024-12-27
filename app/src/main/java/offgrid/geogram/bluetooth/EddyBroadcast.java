package offgrid.geogram.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import offgrid.geogram.core.Log;

public class EddyBroadcast {

    private static final String TAG = "EddyBroadcast";

    private static EddyBroadcast instance; // Singleton instance
    private final Context context;

    // UUIDs for Eddystone services and characteristics
    private static final UUID EDDYSTONE_SERVICE_UUID = UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
    private static final UUID MESSAGE_CHARACTERISTIC_UUID = UUID.fromString("abcdef12-3456-7890-abcd-ef1234567890");

    private final AppBluetoothGattServer gattServer;
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();

    private EddyBroadcast(Context context) {
        this.context = context.getApplicationContext(); // Use application context to avoid leaks
        this.gattServer = AppBluetoothGattServer.getInstance(context); // Reuse GATT server
    }

    /**
     * Singleton access to the EddyBroadcast instance.
     */
    public static synchronized EddyBroadcast getInstance(Context context) {
        if (instance == null) {
            instance = new EddyBroadcast(context);
        }
        return instance;
    }

    /**
     * Broadcasts a message to all connected devices.
     */
    public void broadcastMessageToAll(String message) {
        List<BluetoothDevice> connectedDevices = gattServer.getConnectedDevices();
        if (connectedDevices.isEmpty()) {
            Log.i(TAG, "No connected devices to send the message.");
            return;
        }

        for (BluetoothDevice device : connectedDevices) {
            sendMessageToDevice(device, message);
        }
    }

    /**
     * Sends a message to a specific Eddystone device and waits for a response.
     */
    public void sendMessageToDeviceAndReceiveResponse(BluetoothDevice device, String message, ResponseCallback callback) {
        if (device == null) {
            Log.i(TAG, "Device is null. Cannot send message.");
            return;
        }

        try {
            BluetoothGattCharacteristic characteristic = gattServer.getCharacteristic(EDDYSTONE_SERVICE_UUID, MESSAGE_CHARACTERISTIC_UUID);
            if (characteristic == null) {
                Log.i(TAG, "Message characteristic not found for device: " + device.getAddress());
                return;
            }

            characteristic.setValue(message.getBytes());
            boolean success = gattServer.writeCharacteristic(device, characteristic);
            if (success) {
                Log.i(TAG, "Message sent to device: " + device.getAddress());
                String response = gattServer.readCharacteristic(device, characteristic);
                callback.onResponse(response);
            } else {
                Log.i(TAG, "Failed to send message to device: " + device.getAddress());
            }
        } catch (Exception e) {
            Log.i(TAG, "Unexpected error while communicating with device: " + e.getMessage());
        }
    }

    /**
     * Helper method to send a message to a specific device.
     */
    private void sendMessageToDevice(BluetoothDevice device, String message) {
        sendMessageToDeviceAndReceiveResponse(device, message, response -> {
            Log.i(TAG, "Response from " + device.getAddress() + ": " + response);
        });
    }

    public interface ResponseCallback {
        void onResponse(String response);
    }
}
