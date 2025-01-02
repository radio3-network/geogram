package offgrid.geogram.bluetooth.broadcast;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import offgrid.geogram.bluetooth.BluetoothCentral;
import offgrid.geogram.core.Log;

/*

    Question: How to send a broadcast message to all connected devices?

    Answer: We only connect to devices found inside BeaconListing.java
    inside the HashMap<String, BeaconReachable> beacons array of objects.


    === Sender side ===

    1) A broadcast chat starts at BroadcastChatFragment when the user clicks
    on the "Send" button with a text inside.

    2) A BroadcastMessage messageToBroadcast is created with the text of
    the message and added to our internal list of messages

    3) A thread is launched to send the message via BroadcastChat.broadcast()
    This way the sending of message will not stop the UI responsiveness.
    The method called is BroadcastChat.broadcast(message, getContext());

    4) BroadcastChat.broadcast(message, getContext()) will check if bluetooth
    is available

    5) From there it will go to BlueCentral and message all connected devices
    using broadcastMessageToAllEddystoneDevices(String message).

    6) A message of type B: (broadcast) is created and is called from Bluecomm.java
    Bluecomm.getInstance(this.context).writeData(device.getMacAddress(), text);

    7) There is a delay of typically 500 milliseconds to permit the other device
    to receive the message. This is done for all devices, there is no waiting for
    acknowledgement that the other device received the message.



     === Receiver side ===

     1) Incoming messages are captured at AppBluetoothGattServer because it runs
     a GATT server that is pooling for incoming messages. When a new message
     arrives then it triggers the onCharacteristicWriteRequest() that will then
     call the BlueCentral central = BlueCentral.getInstance(); and send data to
     to central.receivingDataFromDevice(device.getAddress(), received, context);

     2) that receivingDataFromDevice() method will ship the request for processing:
     String answer = processReceivedRequest(macAddress, receivedData, context);
     The processing of this message is not yet threaded, so it might get the UI
     stuck in waiting for a reply. It also isn't prepared for multipart messages.

     This processReceivedRequest() is where the command and parameters are executed
     and writes the message on the broadcast queue of messages

     3) BlueCentral creates a sender: requestData = BlueRequestData.createSender(answer);
     This is originally intended for sending back data to the requester device when
     performing a read operation, and it is prepared for multipart messages

    4) the request is placed on the queue: requests.put(macAddress, requestData);

    5) At this point it will be removed after 3 minutes by cleanupOldRequests().
    There is nothing happening since nobody is expecting to handle those requests

 */

public class BroadcastChat {
    private static final String TAG_ID = "offgrid-broadcast";

    // Messages that were broadcast
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
