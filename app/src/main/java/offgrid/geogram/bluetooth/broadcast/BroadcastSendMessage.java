package offgrid.geogram.bluetooth.broadcast;

import static offgrid.geogram.bluetooth.comms.Bluecomm.gapBroadcast;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;

import offgrid.geogram.bluetooth.BeaconFinder;
import offgrid.geogram.bluetooth.BluetoothCentral;
import offgrid.geogram.bluetooth.comms.BluePackage;
import offgrid.geogram.bluetooth.comms.Bluecomm;
import offgrid.geogram.bluetooth.comms.DataType;
import offgrid.geogram.core.Central;
import offgrid.geogram.core.Log;
import offgrid.geogram.core.old.old.GenerateDeviceId;
import offgrid.geogram.database.BioProfile;
import offgrid.geogram.settings.SettingsUser;
import offgrid.geogram.things.BeaconReachable;

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

    5) From there it will launch a message all connected devices
    using broadcastMessageToAllEddystoneDevices(String message).

    6) A message of type B: (broadcast) is created and is called from Bluecomm.java
    Bluecomm.getInstance(this.context).writeData(device.getMacAddress(), text);

    7) There is a delay of typically 500 milliseconds to permit the other device
    to receive the message. This is done for all devices, there is no waiting for
    acknowledgement that the other device received the message.



     === Receiver side ===

     1) Incoming messages are captured at GattServer because it runs
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

public class BroadcastSendMessage {
    private static final String TAG_ID = "BroadcastSendMessage";

    // These are the messages visible from the UI
    // This gets updated even when the UI is not selected
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
     * @param messageToBroadcast a short text message
     * @return false when something went wrong (e.g., Bluetooth not available)
     */
    public static boolean broadcast(BroadcastMessage messageToBroadcast, Context context) {

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
        broadcastMessageToAllEddystoneDevices(messageToBroadcast, context);
        Log.i(TAG_ID, "Message sent to all Eddystone devices: " + messageToBroadcast.getMessage());

        return true;
    }

    /**
     * Broadcasts a message to all Eddystone devices in reach.
     * @param messageToBroadcast The message to be sent, it needs to be within 20 characters
     */
    public static void broadcastMessageToAllEddystoneDevices(BroadcastMessage messageToBroadcast, Context context) {
        Thread thread = new Thread(() -> {
            try {
                Collection<BeaconReachable> devices = BeaconFinder.getInstance(context).getBeaconMap().values();
                if (devices.isEmpty()) {
                    Log.i(TAG_ID, "No Eddystone devices to broadcast the message.");
                    return;
                }
                // create the package to send
                BluePackage packageToSend = BluePackage.createSender(DataType.B, messageToBroadcast.getMessage());
                messageToBroadcast.setPackage(packageToSend);
                // iterate over the devices
                for (BeaconReachable device : devices) {
                    // send the message
                    sendPackageToDevice(device, packageToSend, context);
                }
                Thread.sleep(500); // Pause for a bit
            } catch (InterruptedException e) {
                Log.e(TAG_ID, "Thread interrupted: " + e.getMessage());
            }

        });
        thread.start(); // Starts the thread

    }

    /**
     * This is used for asking the other device to send a parcel that is missing
     * @param macAddress MAC address of the device
     * @param gapIndex index of the missing parcel
     * @param context application context
     */
    public static void sendParcelToDevice(String macAddress,
                                          String gapIndex,
                                          Context context) {
        // create a single broadcast command
        String text = gapBroadcast + gapIndex;
        // send it over the wire
        Log.i(TAG_ID, "GapData: Sending gap data request to " + macAddress + " with: " + text);
        Bluecomm.getInstance(context).writeData(macAddress, text);
    }

    /**
     * Sends a message to a specific Eddystone device.
     * When the message is large, it will break into multiple portions
     * @param device The Eddystone device to send the message to
     * @param packageToSend The message to be sent
     * @param context The application context
     */
    private static void sendPackageToDevice(BeaconReachable device,
                                            BluePackage packageToSend,
                                            Context context) {
        try {
            // reset the counter for this package
            packageToSend.resetParcelCounter();
            // send all parcels of the package to the other device
            for(int i = 0; i <= packageToSend.getMessageParcelsTotal(); i++){
                String text = packageToSend.getNextParcel();
                Log.i(TAG_ID, "Sending message to " + device.getMacAddress() + " with data: " + text);
                Bluecomm.getInstance(context).writeData(device.getMacAddress(), text);
                Thread.sleep(500);
            }
            Log.i(TAG_ID, "Message sent to Eddystone device: " + device.getMacAddress());
            //Thread.sleep(500);

        } catch (InterruptedException e) {
            Log.e(TAG_ID, "Thread sleep interrupted: " + e.getMessage());
        }
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

    /**
     * Will broadcast the profile of this device to everyone within reach
     */
    public static void sendProfileToEveryone(Context context) {
        SettingsUser settings = Central.getInstance().getSettings();
        BioProfile profile = new BioProfile();
        profile.setNick(settings.getNickname());
        String deviceId = GenerateDeviceId.generateInstanceId(context);
        profile.setId(deviceId);
        profile.setColor(settings.getPreferredColor());
        //profile.setNpub(settings.getNpub());

        String message = "/bio:" + profile.toJson();
        BroadcastMessage messageToBroadcast = new BroadcastMessage(message, deviceId, true);
        BroadcastSendMessage.broadcastMessageToAllEddystoneDevices(messageToBroadcast, context);
    }

}
