package offgrid.geogram.bluetooth.broadcast;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import offgrid.geogram.bluetooth.BluetoothCentral;
import offgrid.geogram.core.Log;

public class BroadcastChat {
    private static final String TAG_ID = "offgrid-broadcast";

    // Messages that were broadcasted
    public static ArrayList<BroadcastMessage> messages = new ArrayList<>();

    // Listener for message updates
    private static WeakReference<MessageUpdateListener> messageUpdateListener;

    public static void addMessage(BroadcastMessage message) {
        messages.add(message);
        notifyMessageUpdate();
    }

    /**
     * Tries to send a message to all Eddystone beacon devices in reach.
     *
     * @param message a short text message
     * @return false when something went wrong (e.g., Bluetooth not available)
     */
    public static boolean broadcast(String message, Context context) {

        BluetoothCentral bluetoothCentral = BluetoothCentral.getInstance(context);
        if (bluetoothCentral.isBluetoothAvailable()) {
            // Bluetooth is available and enabled, proceed with operations
            bluetoothCentral.start();
        } else {
            // Bluetooth is either not supported or not enabled
            Log.i(TAG_ID, "Bluetooth is not ready. Please enable it to continue.");
            return false;
        }

        // Save the message in our list
        addMessage(new BroadcastMessage(message, null, true));

        // Send the message
        bluetoothCentral.broadcastMessageToAllEddystoneDevices(message);
        Log.i(TAG_ID, "Message sent to all Eddystone devices: " + message);

        return true;
    }

    /**
     * Sets the listener for message updates.
     */
    public static void setMessageUpdateListener(MessageUpdateListener listener) {
        messageUpdateListener = new WeakReference<>(listener);
    }

    /**
     * Removes the listener for message updates.
     */
    public static void removeMessageUpdateListener() {
        if (messageUpdateListener != null) {
            messageUpdateListener.clear();
            messageUpdateListener = null;
        }
    }

    /**
     * Notifies the listener about message updates.
     */
    private static void notifyMessageUpdate() {
        if (messageUpdateListener != null) {
            MessageUpdateListener listener = messageUpdateListener.get();
            if (listener != null) {
                listener.onMessageUpdate();
            }
        }
    }

    /**
     * Interface for listening to message updates.
     */
    public interface MessageUpdateListener {
        void onMessageUpdate();
    }
}
