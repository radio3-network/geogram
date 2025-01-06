package offgrid.geogram.bluetooth.comms;

import static offgrid.geogram.bluetooth.comms.Bluecomm.timeBetweenChecks;
import static offgrid.geogram.bluetooth.comms.Bluecomm.timeBetweenMessages;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import offgrid.geogram.bluetooth.broadcast.BroadcastMessage;
import offgrid.geogram.core.Log;

/**
 * The queues for outgoing and incoming transmissions.
 * This class is implemented as a singleton to ensure only one instance of the queue exists.
 */
public class BlueQueue {

    private static BlueQueue instance = null;
    private final Context context;

    // The queue for outgoing transmissions
    private final CopyOnWriteArrayList<BlueQueueItem> queueToSend = new CopyOnWriteArrayList<>();
    // queue to store individual one-line messages to be sent
    private Thread queueThreadToSend = null;

    // The queue for incoming transmissions
    //private final CopyOnWriteArrayList<BlueQueueItem> queueToReceive = new CopyOnWriteArrayList<>();
    // queue to store individual one-line messages to be received
    //private Thread queueThreadToReceive = null;

    // Queue for data packages being built with what we receive from other devices
    // <UID, BluePackage>
    public final HashMap<String, BluePackage> packagesBeingReceived = new HashMap<>();

    // Queue for data packages that we are sending to other devices
    // <UID, BluePackage>
    public final HashMap<String, BluePackage> packagesBeingSent = new HashMap<>();

    // These are the messages visible from the UI
    // This gets updated even when the UI is not selected
    public static ArrayList<BroadcastMessage> messagesReceivedAsBroadcast = new ArrayList<>();



    private static final String TAG = "BlueQueues";

    // Private constructor to prevent external instantiation
    private BlueQueue(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Provides the singleton instance of the BlueQueues class.
     *
     * @return The singleton instance of BlueQueues.
     */
    public static BlueQueue getInstance(Context context) {
        if (instance == null) {
            instance = new BlueQueue(context);
        }
        return instance;
    }

    public void start(){
        if(queueThreadToSend == null){
            startThreadToSendMessagesInQueue();
        }
//        if(queueThreadToReceive == null){
//            startThreadToReceiveMessagesInQueue();
//        }
    }

    /**
     * Adds a package to be sent to other devices for future memory
     * in cases where it is needed to send again
     * @param packageToSend the data to be dispatched
     */
    public void addPackageToSending(BluePackage packageToSend){
        String uid = packageToSend.getId();
        if(packagesBeingReceived.containsKey(uid)){
            return;
        }
        Log.i(TAG, "Adding package to archive: " + uid);
        packagesBeingSent.put(uid, packageToSend);
    }

    /**
     * Adds an item to the queue.
     *
     * @param item The BlueQueueItem to be sent elsewhere
     */
    public void addQueueToSend(BlueQueueItem item) {
        queueToSend.add(item);
    }

    /**
     * Adds an item to the queue of received transmissions.
     */
    public void addQueueToReceive(String macAddress, String data) {
        BlueQueueItem item = new BlueQueueItem(macAddress, data);
        queueToSend.add(item);
    }

    /**
     * The thread that keeps sending messages that were placed
     * on the queue
     */
    private void startThreadToSendMessagesInQueue() {
        queueThreadToSend = new Thread(() -> {
            Log.i(TAG, "Starting thread to send messages in queue");
            try {
                while(true) {
                    Thread.sleep(timeBetweenChecks); // Pause for a bit
                    // nothing to send
                    if(queueToSend.isEmpty()){
                        continue;
                    }

                    // don't send messages while we are receiving data
                    if(stillReceivingMessages()){
                        continue;
                    }

                    // there is something to send
                    while(queueToSend.isEmpty() == false){
                        Mutex.getInstance().waitUntilUnlocked();
                        //Mutex.getInstance().lock();
                        BlueQueueItem item = queueToSend.get(0);
                        Bluecomm.getInstance(context).writeData(item);
                        queueToSend.remove(0);
                        //Mutex.getInstance().unlock();
                        Thread.sleep(timeBetweenMessages);
                    }

                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Thread interrupted: " + e.getMessage());
            }

        });
        queueThreadToSend.start(); // Starts the thread
    }

    /**
     * Checks if there are received messages that are
     * still with activity
     * @return true when we are receiving data
     */
    private boolean stillReceivingMessages() {
        for(BluePackage pkg : packagesBeingReceived.values()){
            if(pkg.isStillActive()){
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a specific message is already on the queue to be dispatched
     * to another device and avoid repetition of messages.
     * @param message to be compared
     * @return true when it is a duplicate message
     */
    public boolean isAlreadyOnQueueToSend(String message, String macAdress) {
        for(BlueQueueItem item : queueToSend){
            // needs to match the mac address
            if(item.getMacAddress().equals(macAdress) == false){
                continue;
            }
            // is the data same?
            if(item.getData().equals(message)){
                return true;
            }
        }
        return false;
    }
}
