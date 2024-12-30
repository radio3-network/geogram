package offgrid.geogram.bluetooth.comms;

import java.util.HashMap;

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
        // Initialization logic if needed
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
     * @param address MAC address of the bluetooth device
     * @param received request that was received
     */
    public void startRequest(String address, String received) {
        // avoid null addresses
        if(address == null){
            return;
        }
        // remove previous requests for this address (if any)
        requests.remove(address);
        // now we need to process the request
        String answer = processRequest(received);
        Log.i(TAG, "Request processed for " + address + ". Answer: " + answer);
        // prepare an answer to be shipped
        BlueRequestData requestData = BlueRequestData.createSender(answer);
        // place it on the queue
        requests.put(address, requestData);
        Log.i(TAG, "Request placed on queue: " + address + " - " + received);
    }

    private String processRequest(String received) {
        return "Hello World!";
    }

    /**
     * Provides the request data for the given MAC address.
     * @param address MAC address of the bluetooth device
     * @return the associated request data or null when not found
     */
    public BlueRequestData getRequest(String address){
        return requests.get(address);
    }
}

