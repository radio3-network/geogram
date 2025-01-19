package offgrid.geogram.core;

import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import offgrid.geogram.fragments.DebugFragment;

public class Log {

    private static int sizeOfLog = 500;

    // list of messages received
    public static final CopyOnWriteArrayList<String> logMessages = new CopyOnWriteArrayList<>();



    public static void clear() {
        logMessages.clear();
    }


    public static void log(int priority, String tag, String message) {
        // Get the current timestamp
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
//        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault())
//                .format(Calendar.getInstance().getTime());

        // Format the message with the timestamp
        String formattedMessage = timestamp + " [" + tag + "] " + message;

        // Write to the system log
        android.util.Log.println(priority, tag, formattedMessage);

        // add the message
        logMessages.add(formattedMessage);

        if(logMessages.size() > sizeOfLog){
            logMessages.remove(0);
        }

        DebugFragment.getInstance().logUpdateAllMessages();

//        logWindow.post(() -> {
//            logWindow.setText(currentText);
//            logWindow.setSelection(logWindow.getText().length()); // Auto-scroll
//        });
    }

    public static void d(String tag, String message) {
        log(android.util.Log.DEBUG, tag, message);
    }

    public static void e(String tag, String message) {
        log(android.util.Log.ERROR, tag, message);
    }

    public static void i(String tag, String message) {
        log(android.util.Log.INFO, tag, message);
    }

    // Add other levels if needed (WARN, VERBOSE, etc.)
}
