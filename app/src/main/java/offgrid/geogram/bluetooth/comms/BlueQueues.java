package offgrid.geogram.bluetooth.comms;

import static offgrid.geogram.bluetooth.comms.Bluecomm.timeBetweenChecks;
import static offgrid.geogram.bluetooth.comms.Bluecomm.timeBetweenMessages;

import android.content.Context;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import offgrid.geogram.core.Log;

/**
 * The queues for outgoing and incoming transmissions.
 * This class is implemented as a singleton to ensure only one instance of the queue exists.
 */
public class BlueQueues {

    private static BlueQueues instance = null;
    private final Context context;

    // The queue for outgoing transmissions
    private final CopyOnWriteArrayList<BlueQueueItem> queueToSend = new CopyOnWriteArrayList<>();
    // queue to store individual one-line messages to be sent
    private Thread queueThreadToSend = null;

    // The queue for incoming transmissions
    private final CopyOnWriteArrayList<BlueQueueItem> queueToReceive = new CopyOnWriteArrayList<>();
    // queue to store individual one-line messages to be received
    private Thread queueThreadToReceive = null;

    // Queue for data packages being build from what we receive of other devices
    // <UID, BluePackage>
    public final HashMap<String, BluePackage> writeActions = new HashMap<>();



    private static final String TAG = "BlueQueues";

    // Private constructor to prevent external instantiation
    private BlueQueues(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Provides the singleton instance of the BlueQueues class.
     *
     * @return The singleton instance of BlueQueues.
     */
    public static BlueQueues getInstance(Context context) {
        if (instance == null) {
            instance = new BlueQueues(context);
        }
        return instance;
    }

    public void start(){
        if(queueThreadToSend == null){
            startThreadToSendMessagesInQueue();
        }
        if(queueThreadToReceive == null){
            startThreadToReceiveMessagesInQueue();
        }
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
    private void startThreadToReceiveMessagesInQueue() {
        queueThreadToReceive = new Thread(() -> {
            Log.i(TAG, "Starting thread to receive messages in queue");
            try {
                while(true) {
                    Thread.sleep(timeBetweenChecks); // Pause for a bit
                    // nothing to send
                    if(queueToReceive.isEmpty()){
                        continue;
                    }
                    // there is something to send
                    while(queueToReceive.isEmpty() == false){
                        BlueQueueItem item = queueToReceive.get(0);
                        //Bluecomm.getInstance(context).writeData(item);
                        queueToReceive.remove(0);
                        Thread.sleep(timeBetweenMessages);
                    }

                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Thread interrupted: " + e.getMessage());
            }

        });
        queueThreadToReceive.start(); // Starts the thread
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

}
