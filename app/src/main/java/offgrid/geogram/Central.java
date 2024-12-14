package offgrid.geogram;

import static offgrid.geogram.core.Messages.log;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import offgrid.geogram.core.BackgroundService;
import offgrid.geogram.old.WiFi_control;

public class Central {

    public static WiFi_control wifiControl;
    public static boolean alreadyStarted = false;
    public static String device_name = null;
    public static boolean hasNeededPermissions = false;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public Central(String name) {
    }


    public static void initializeWiFiControl(Context context) {
        if(alreadyStarted){
            return;
        }
        wifiControl = new WiFi_control(context);
        wifiControl.startAdvertising();
        wifiControl.startDiscovery();

        alreadyStarted = true;
    }

    public static boolean wifi_setup(AppCompatActivity act, String TAG) {
        // Check and request permissions
        if(hasNeededPermissions == false){
            Central.message(act, TAG,"Failed to get necessary permissions");
            return false;
        }else{
            log(TAG, "Necessary permissions available");
        }

        // start the background service
        log(TAG, "Starting the background service");
        Intent serviceIntent = new Intent(act, BackgroundService.class);
        ContextCompat.startForegroundService(act, serviceIntent);
        return true;
    }



//    public static void log(String TAG, String message) {
//        Log.d(TAG, message);
//    }

    public static void message(AppCompatActivity act, String TAG, String message) {
        Toast.makeText(act, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }


}
