package offgrid.geogram.bluetooth.comms;

import android.content.Context;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import offgrid.geogram.bluetooth.BeaconFinder;
import offgrid.geogram.bluetooth.broadcast.BroadcastChat;
import offgrid.geogram.bluetooth.broadcast.BroadcastMessage;
import offgrid.geogram.core.Central;
import offgrid.geogram.core.Log;

/**
 * This class stores the requests that are made from outside
 * devices into this one. Since messages need to be shipped
 * in small little packages, this is where we keep track of
 * them.
 */
public class BlueFromOutside {

    // the requests currently active
    private final HashMap<String, BluePackage> requests = new HashMap<>();

    // Static instance of the singleton
    private static BlueFromOutside instance;
    private static final String TAG = "BlueCentral";

    // Private constructor to prevent instantiation from outside
    private BlueFromOutside() {
        startCleanupThread(); // Start cleanup thread upon initialization
    }

    /**
     * Provides the global instance of the BlueCentral class.
     *
     * @return The singleton instance of BlueCentral.
     */
    public static synchronized BlueFromOutside getInstance() {
        if (instance == null) {
            instance = new BlueFromOutside();
        }
        return instance;
    }

    /**
     * A new request has arrived. First we run the request
     * and get the result. After that part we will ship the
     * results to the other device.
     * Any previous requests from the same MAC address will
     * be erased from the queue.
     *
     * @param macAddress MAC address of the bluetooth device
     * @param receivedData request that was received
     */
    public void receivingDataFromDevice(String macAddress, String receivedData, Context context) {
        // avoid null addresses
        if (macAddress == null) {
            return;
        }
        // process the request
        String answer = processReceivedRequest(macAddress, receivedData, context);
        Log.i(TAG, "Request processed from " + macAddress + ". Answer: " + answer);
        // prepare an answer to be shipped
        BluePackage requestData = BluePackage.createSender(answer);
        // remove previous requests for this address (if any)
        requests.remove(macAddress);
        // place it on the queue
        requests.put(macAddress, requestData);
        Log.i(TAG, "Request placed on queue: " + macAddress + " - " + receivedData);
    }

    /**
     * Process all commands arriving to the
     * @param received
     * @return
     */
    private String processReceivedRequest(String macAddress, String received, Context context) {
        DataTypes command;

        try{
            // is this a valid command?
            if(received.contains(":")){
                // it has multiple parts inside
                String[] data = received.split(":");
                command = DataTypes.valueOf(data[0]);
            }else{
                // single command
                command = DataTypes.valueOf(received);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid command: " + received);
            return "Invalid command";
        }
        Log.i(TAG, "Received command: " + received);

        switch (command) {
            case G -> {
                return getUserFromDevice();
            }
            case B -> {
                return receiveBroadCastMessage(macAddress, received, context);
            }
        }


        return "Unknown request";
    }

    private String receiveBroadCastMessage(String macAddress, String receivedText, Context context) {
        String key = DataTypes.B.toString() + ":";
        if(!receivedText.contains(key)){
            return "Invalid broadcast message";
        }
        String messageText = receivedText.substring(key.length());
        // this message was written by someone else
        BroadcastMessage messageReceived = new BroadcastMessage(messageText, macAddress, false);
        String deviceId = BeaconFinder.getInstance(context).getDeviceId(macAddress);
        if(deviceId != null){
            messageReceived.setDeviceId(deviceId);
        }
        // place the message on the list
        BroadcastChat.messages.add(messageReceived);

        return "Received";
    }

    private String getUserFromDevice() {
        return Central.getInstance().getSettings().getNickname();
    }

    /**
     * Provides the request data for the given MAC address.
     * @param address MAC address of the bluetooth device
     * @return the associated request data or null when not found
     */
    public BluePackage getRequest(String address) {
        return requests.get(address);
    }

    /**
     * Starts a background thread to clean up outdated requests
     * that are older than 5 minutes (300,000 milliseconds).
     */
    private void startCleanupThread() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Check every minute
                    cleanupOldRequests();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Cleanup thread interrupted: " + e.getMessage());
                    break;
                }
            }
        }).start();
    }

    /**
     * Removes requests that are older than 5 minutes.
     */
    private synchronized void cleanupOldRequests() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, BluePackage>> iterator = requests.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, BluePackage> entry = iterator.next();
            BluePackage requestData = entry.getValue();
            if (currentTime - requestData.getTransmissionStartTime() > 300000) { // 5 minutes
                Log.i(TAG, "Removing outdated request for address: " + entry.getKey());
                iterator.remove();
            }
        }
    }
}
