package offgrid.geogram.bluetooth.comms;

import static offgrid.geogram.bluetooth.comms.Bluecomm.gapBroadcast;

import android.content.Context;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import offgrid.geogram.bluetooth.broadcast.BroadcastSendMessage;
import offgrid.geogram.bluetooth.broadcast.BroadcastMessage;
import offgrid.geogram.core.Log;
import offgrid.geogram.database.BioDatabase;
import offgrid.geogram.database.BioProfile;

/**
 * This class stores the requests that are made from outside
 * devices into this one. Since messages need to be shipped
 * in small little packages, this is where we keep track of
 * them.
 */
public class BlueDataWriteFromOutside {

    // the requests currently active
    private final HashMap<String, BluePackage> requests = new HashMap<>();

    // the write actions from outside devices
    // <UID, BluePackage>
    private final HashMap<String, BluePackage> writeActions = new HashMap<>();


    // Static instance of the singleton
    private static BlueDataWriteFromOutside instance;
    private static final String TAG = "BlueDataWriteFromOutside";

    // Private constructor to prevent instantiation from outside
    private BlueDataWriteFromOutside() {
        startCleanupThread(); // Start cleanup thread upon initialization
    }

    /**
     * Provides the global instance of the BlueCentral class.
     *
     * @return The singleton instance of BlueCentral.
     */
    public static synchronized BlueDataWriteFromOutside getInstance() {
        if (instance == null) {
            instance = new BlueDataWriteFromOutside();
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
            Log.e(TAG, "Null MAC address received");
            return;
        }

        // output a log of what is received
        Log.i(TAG, "Received data from " + macAddress + ": " + receivedData);

        // needs to always contain an :
        if(receivedData == null || !receivedData.contains(":")){
            Log.e(TAG, "Invalid data received: " + receivedData);
            return;
        }

        // is this a single command?
        if(receivedData.startsWith(">")){
            processSingleCommandReceived(macAddress, receivedData, context);
            return;
        }

        String[] data = receivedData.split(":");
        String UID = data[0].substring(0, 2);

        // with a valid device, is there already a write request?
        BluePackage writeAction = null;
        if(writeActions.containsKey(UID)){
            writeAction = writeActions.get(UID);
        }else{
            writeAction = BluePackage.createReceiver(receivedData);
            // first message should be a header, is it valid?
            if(writeAction.isValidHeader()){
                writeActions.put(UID, writeAction);
                // no need to continue, first message is the header
                return;
            }else{
                Log.e(TAG, "Invalid header received for write operation: " + receivedData);
                return;
            }
        }
        // at this point our write action should NOT be null
        if(writeAction == null){
            Log.e(TAG, "Null write action received");
            return;
        }

        // avoid replay actions, this parcel is complete
        if(writeAction.allParcelsReceivedAndValid()){
            return;
        }

        // next messages should be an increment
        writeAction.receiveParcel(receivedData);

        if(writeAction.hasGaps()){
            // get the first gap that is missing
            String gapIndex = writeAction.getFirstGapParcel();
            // send this request back to the other device
            BroadcastSendMessage.sendParcelToDevice(macAddress, gapIndex, context);
            // all done
            return;
        }

        // when the message is complete, process the command inside
        if(writeAction.allParcelsReceivedAndValid()){
            processReceivedRequest(macAddress, writeAction, context);
            Log.i(TAG, "Request processed from " + macAddress);
            // remove this message from the queue
            writeActions.remove(UID);
        }

//        // process the request
//        String answer = processReceivedRequest(macAddress, receivedData, context);
//        Log.i(TAG, "Request processed from " + macAddress + ". Answer: " + answer);
//        // prepare an answer to be shipped
//        BluePackage requestData = BluePackage.createSender(answer);
//        // remove previous requests for this address (if any)
//        requests.remove(macAddress);
//        // place it on the queue
//        requests.put(macAddress, requestData);
//        Log.i(TAG, "Request placed on queue: " + macAddress + " - " + receivedData);
    }

    /**
     * A single command starting with > was received
     * @param macAddress MAC address of the device which sent the message
     * @param receivedData message received
     * @param context context of the application
     */
    private void processSingleCommandReceived(String macAddress, String receivedData, Context context) {
        // example of what we expect to receive
        // >B:XY001
        // this means: broadcast message with identification XY requests parcel 1
        if(receivedData.startsWith(gapBroadcast)){
            String parcelId = receivedData.substring(3);
            // what do we do now?
            String id = parcelId.substring(0, 2);
            String parcelNumber = parcelId.substring(2);
            Log.i(TAG, "Gap data: received request with id: "
                    + id + " for parcel " + parcelNumber);
            // send back the parcel to the other device
            BluePackage writeAction = writeActions.get(id);
            if(writeAction == null){
                Log.e(TAG, "GapData: No write action found for id: " + id);
                return;
            }
            int value;
            try {
                value = Integer.parseInt(parcelNumber);
            } catch (NumberFormatException e){
                Log.e(TAG, "GapData: Invalid parcel number received: " + parcelNumber);
                return;
            }
            String textToSendAgain = writeAction.getSpecificParcel(value);
            Log.i(TAG, "GapData: Sending parcel: " + textToSendAgain);
            Bluecomm.getInstance(context).writeData(macAddress, textToSendAgain);
        }

        // single command is not yet supported
        Log.i(TAG, "Single command received and ignored: " + receivedData);

    }

    /**
     * Process all commands arriving to the
     * @return the answer to the request
     */
    private void processReceivedRequest(
            String macAddress,
            BluePackage writeAction,
            Context context) {


        //Log.i(TAG, "Received command: " + received);

        switch (writeAction.getCommand()) {
            case G -> {
            }
            case B -> {
                writeBroadCastMessage(macAddress, writeAction, context);
            }
            default -> {
            }
        }
    }

    /**
     * Handle the broadcast message when being received from the outside
     * @param context
     * @return
     */
    private void writeBroadCastMessage(
            String macAddress,
            BluePackage writeAction,
            Context context) {

        String messageText = writeAction.getData();

        // this message was written by someone else
        BroadcastMessage messageReceived = new BroadcastMessage(messageText, macAddress, false);
        messageReceived.setDeviceId(macAddress);
        // place the message on the list
        BroadcastSendMessage.messages.add(messageReceived);
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
