package offgrid.geogram.bluetooth;

import static offgrid.geogram.bluetooth.Bluecomm.timeBetweenChecks;
import static offgrid.geogram.bluetooth.Bluecomm.timeBetweenMessages;

import android.content.Context;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import offgrid.geogram.bluetooth.other.comms.BluePackage;
import offgrid.geogram.bluetooth.other.comms.BlueQueueParcel;
import offgrid.geogram.bluetooth.other.comms.Mutex;
import offgrid.geogram.core.Log;

/**
 * The queues for outgoing and incoming transmissions.
 * This class is implemented as a singleton to ensure only one instance of the queue exists.
 */
public class BlueQueueSending {

    // Queue for outgoing packages
    // <UID, BluePackage> // UID is the random identifier for the package
    // this is only used as archive, therefore has no MAC addresses inside the package
    public final HashMap<String, BluePackage> packagesToSend = new HashMap<>();

    // Queue for outgoing parcels
    private final CopyOnWriteArrayList<BlueQueueParcel>
            queueParcelToSend = new CopyOnWriteArrayList<>();


    // thread to send messages when possible
    private Thread queueThreadToSend = null;
    private static final String TAG = "BlueQueueSending";
    private static BlueQueueSending instance = null;
    private final Context context;

    // Private constructor to prevent external instantiation
    private BlueQueueSending(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Provides the singleton instance of the BlueQueues class.
     *
     * @return The singleton instance of BlueQueues.
     */
    public static BlueQueueSending getInstance(Context context) {
        if (instance == null) {
            instance = new BlueQueueSending(context);
        }
        return instance;
    }

    public void start(){
        if(queueThreadToSend == null){
            startThreadToSendMessagesInQueue();
        }
    }

    /**
     * Adds a package to be sent to other devices for future memory
     * in cases where it is needed to send again
     * @param packageToSend the data to be dispatched
     */
    public void addPackageToSend(BluePackage packageToSend){
        String uid = packageToSend.getId();
        HashMap<String, BluePackage> packagesReceivedRecently
                = BlueQueueReceiving.getInstance(context).packagesReceivedRecently;
        // avoid duplicates
        if(packagesReceivedRecently.containsKey(uid)){
            return;
        }
        Log.i(TAG, "Adding package to send: " + uid);
        packagesToSend.put(uid, packageToSend);
    }

    /**
     * Adds an item to the queue.
     *
     * @param item The BlueQueueItem to be sent elsewhere
     */
    public void addQueueToSend(BlueQueueParcel item) {
        queueParcelToSend.add(item);
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
                    if(queueParcelToSend.isEmpty()){
                        continue;
                    }

                    // don't send messages while we are receiving data
                    if(BlueQueueReceiving.getInstance(context).stillReceivingMessages()){
                        continue;
                    }

                    // there is something to send
                    while(queueParcelToSend.isEmpty() == false){
                        //Mutex.getInstance().waitUntilUnlocked();
                        //Mutex.getInstance().lock();
                        try {
                            // get the oldest parcel on the queue
                            BlueQueueParcel item = queueParcelToSend.get(0);
                            // send it to the target device
                            Bluecomm.getInstance(context).writeData(item);
                            // remove the item from the queue
                            queueParcelToSend.remove(0);
                        }catch (Exception e){
                            Log.e(TAG, "Error sending data: " + e.getMessage());
                        }
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
     * Checks if a specific message is already on the queue to be dispatched
     * to another device and avoid repetition of messages.
     * @param message to be compared
     * @return true when it is a duplicate message
     */
    public boolean isAlreadyOnQueueToSend(String message, String macAddress) {
        for(BlueQueueParcel item : queueParcelToSend){
            // needs to match the mac address
            if(item.getMacAddress().equals(macAddress) == false){
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
