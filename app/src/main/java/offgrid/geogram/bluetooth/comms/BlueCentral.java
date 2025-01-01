package offgrid.geogram.bluetooth.comms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
public class BlueCentral {

    // the requests currently active
    private final HashMap<String, BlueRequestData> requests = new HashMap<>();

    // Static instance of the singleton
    private static BlueCentral instance;
    private static final String TAG = "BlueCentral";

    // Private constructor to prevent instantiation from outside
    private BlueCentral() {
        startCleanupThread(); // Start cleanup thread upon initialization
    }

    /**
     * Provides the global instance of the BlueCentral class.
     *
     * @return The singleton instance of BlueCentral.
     */
    public static synchronized BlueCentral getInstance() {
        if (instance == null) {
            instance = new BlueCentral();
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
    public void receivingDataFromDevice(String macAddress, String receivedData) {
        // avoid null addresses
        if (macAddress == null) {
            return;
        }
        // remove previous requests for this address (if any)
        requests.remove(macAddress);
        // now we need to process the request
        String answer = processReceivedRequest(macAddress, receivedData);
        Log.i(TAG, "Request processed from " + macAddress + ". Answer: " + answer);
        // prepare an answer to be shipped
        BlueRequestData requestData = BlueRequestData.createSender(answer);
        // place it on the queue
        requests.put(macAddress, requestData);
        Log.i(TAG, "Request placed on queue: " + macAddress + " - " + receivedData);
    }

    /**
     * Process all commands arriving to the
     * @param received
     * @return
     */
    private String processReceivedRequest(String address, String received) {
        RequestTypes command;

        try{
            // is this a valid command?
            if(received.contains(":")){
                // it has multiple parts inside
                String[] data = received.split(":");
                command = RequestTypes.valueOf(data[0]);
            }else{
                // single command
                command = RequestTypes.valueOf(received);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid command: " + received);
            return "Invalid command";
        }
        Log.i(TAG, "Received command: " + received);

        switch (command) {
            case GET_USER_FROM_DEVICE -> {
                return getUserFromDevice();
            }
            case B -> {
                return receiveBroadCastMessage(address, received);
            }
        }


        return "Unknown request";
    }

    private String receiveBroadCastMessage(String address, String received) {
        String key = RequestTypes.B.toString() + ":";
        if(!received.contains(key)){
            return "Invalid broadcast message";
        }
        String messageText = received.substring(key.length());
        // this message was written by someone else
        BroadcastMessage messageObject = new BroadcastMessage(messageText, address, false);
        // place the message on the list
        BroadcastChat.messages.add(messageObject);

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
    public BlueRequestData getRequest(String address) {
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
        Iterator<Map.Entry<String, BlueRequestData>> iterator = requests.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, BlueRequestData> entry = iterator.next();
            BlueRequestData requestData = entry.getValue();
            if (currentTime - requestData.getTransmissionStartTime() > 300000) { // 5 minutes
                Log.i(TAG, "Removing outdated request for address: " + entry.getKey());
                iterator.remove();
            }
        }
    }
}
