package offgrid.geogram.core;

import android.widget.EditText;

public class Log {

    // the full log text
    private static String currentText = "";

    private static EditText logWindow;

    public static void setLogWindow(EditText editText) {
        logWindow = editText;
    }

    public static void clear() {
        currentText = "";
        logWindow.setText("");
    }

    public static void logUpdate(){
        logWindow.post(() -> {
            logWindow.setText(currentText);
            logWindow.setSelection(logWindow.getText().length()); // Auto-scroll
        });
    }

    public static void log(int priority, String tag, String message) {
        // Write to the system log
        android.util.Log.println(priority, tag, message);

        // archive the current text
        currentText += "\n" + "[" + tag + "] " + message;
        if (currentText.length() > 5000) { // Example: Keep last 5000 characters
            currentText = currentText.substring(currentText.length() - 5000);
        }

        // Append the message to the log window
        if (logWindow == null) {
            return;
        }
        logWindow.post(() -> {
                logWindow.setText(currentText);
                logWindow.setSelection(logWindow.getText().length()); // Auto-scroll
            });
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
