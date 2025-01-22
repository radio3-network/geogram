package offgrid.geogram.wifi.comm;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CopyOnWriteArrayList;

import offgrid.geogram.core.Log;
import offgrid.geogram.wifi.messages.Message;

/**
 * Dump all incoming Wi-Fi messages here
 * and they will magically be routed to
 * the right location.
 */
public class WiFiReceiver {

    private static final String TAG = "WiFiReceiver";;
    // messages that we receive over WiFi
    public static CopyOnWriteArrayList<Message> messagesReceived = new CopyOnWriteArrayList<>();

    // Handler to run periodic tasks
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final int CHECK_INTERVAL_MS = 1000; // 1 second

    private static final Runnable messageProcessor = new Runnable() {
        @Override
        public void run() {
            // Process messages
            processMessages();

            // Schedule the next check
            handler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    static {
        // Start the periodic check for new messages
        handler.postDelayed(messageProcessor, CHECK_INTERVAL_MS);
    }

    /**
     * Connect to the local Wi-Fi sender to pretend this
     * is a remote device sender. This is used only for
     * testing purposes and development of the message API.
     */
    public static void syncLocally() {
        // get the local messages
        CopyOnWriteArrayList<WiFiMessage> messagesToReceive
                = WiFiSender.getInstance(null).getMessages();
        // add them to the received queue
        for (WiFiMessage messageWiFi : messagesToReceive) {
            processReceivedMessage(messageWiFi.message);
        }
    }

    private static void processMessages() {
        while (messagesReceived.isEmpty() == false) {
            Message message = messagesReceived.remove(0);
            processMessage(message);
            // remove the message from the queue
            messagesReceived.remove(message);
        }
    }

    /**
     * Process the message internally
     * @param message message that was in the queue,
     *                it will be removed after running
     *                this method.
     */
    private static void processMessage(Message message) {

        switch(message.getCid()){
            case HELLO: return;
            case HELLO_REPLY:;return;
            default:
                Log.e(TAG, "Unknown message: " + message.getCid());
        }

    }

    private static Message processReceivedMessage(Message message) {



        for (Message messageExisting : messagesReceived) {
            // only accept one message with the same timestamp to reduce duplicates
            if (messageExisting.getTimeStamp() == message.getTimeStamp()) {
                return;
            }
        }
        // Add the message to the queue
        messagesReceived.add(message);
    }



}
