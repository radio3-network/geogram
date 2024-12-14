package offgrid.geogram.core;

import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Messages {

    private static final String TAG = "offgrid";

    public static void log(String message) {
        Log.d(TAG, message);
    }

    public static void log(String TAG, String message) {
        Log.d(TAG, message);
    }

    public static void message(AppCompatActivity app, String message) {
        Toast.makeText(app, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

}
