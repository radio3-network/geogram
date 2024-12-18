package offgrid.geogram.core;

import static offgrid.geogram.MainActivity.activity;
import static offgrid.geogram.core.Messages.log;
import static offgrid.geogram.wifi.WiFiCommon.peers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import offgrid.geogram.MainActivity;
import offgrid.geogram.R;
import offgrid.geogram.bluetooth.BeaconDefinitions;
import offgrid.geogram.bluetooth.EddystoneBeacon;
import offgrid.geogram.server.SimpleSparkServer;
import offgrid.geogram.wifi.WiFiDirectAdvertiser;
import offgrid.geogram.wifi.WiFiDirectDiscovery;

public class BackgroundService extends Service {

    private static final String TAG_ID = "offgrid-service";
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private Handler handler;
    private Runnable logTask;

    private WiFiDirectAdvertiser wifiDirectAdvertiser; // Wi-Fi Direct Advertiser instance
    private WiFiDirectDiscovery discover = null;
    private EddystoneBeacon eddystoneBeacon = null;

    // how long between scans
    private long interval_seconds = 10;

    private boolean
            wifi_advertise = true,
            wifi_discover = true,
            hasNecessaryPermissions = false;


    @Override
    public void onCreate() {
        super.onCreate();
        log(TAG_ID, "Creating the background service");

        // Create notification channel for Android 8.0+ (API 26)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Offgrid phone, looking for data and connections");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                log(TAG_ID, "Notification channel created");
            } else {
                log(TAG_ID, "NotificationManager is null");
            }
        }

        hasNecessaryPermissions = PermissionsHelper.requestPermissionsIfNecessary(activity);


        // Check and request permissions again
        if (!hasNecessaryPermissions) {
            log(TAG_ID, "Permissions are not granted yet. Waiting for user response.");
            hasNecessaryPermissions = PermissionsHelper.requestPermissionsIfNecessary(activity);
        }

        // needs to have permissions by now
        if (!hasNecessaryPermissions) {
            Messages.message(activity, "Please enable the app permissions");
            return;
        }


//        // Initialize Wi-Fi Direct Advertiser
//        startWiFiAdvertise();
//
//        // Initialize the discovery of other apps
//        startWiFiDiscover();

        // initialize the bluetooth beacon
        startBluetoothBeacon();


    }

    private void startBluetoothBeacon() {
        // Initialize the EddystoneBeacon
        eddystoneBeacon = new EddystoneBeacon(this);
        // Start advertising
        eddystoneBeacon.startAdvertising(BeaconDefinitions.namespaceId);
    }

    /**
     * Starts the Wi-Fi Direct discovery
     */
    private void startWiFiDiscover() {
        if(wifi_discover) {
            log(TAG_ID, "WiFi discovery initialized");
            discover = new WiFiDirectDiscovery(this);
            //discover.registerIntents();
            //discover.startDiscovery(1);
        }else{
            log(TAG_ID, "WiFi discover disabled");
        }
    }

    private void startWiFiAdvertise() {
        if(wifi_advertise){
            wifiDirectAdvertiser = new WiFiDirectAdvertiser(this);
            wifiDirectAdvertiser.startAdvertising();
            log(TAG_ID, "WiFi advertise initialized");
        }else{
            log(TAG_ID, "WiFi advertise disabled");
        }
    }

    /**
     * The method with the loop that runs ever NN seconds
     *
     * @param intent The Intent supplied to {@link android.content.Context#startService},
     * as given.  This may be null if the service is being restarted after
     * its process has gone away, and it had previously returned anything
     * except {@link #START_STICKY_COMPATIBILITY}.
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to
     * start.  Use with {@link #stopSelfResult(int)}.
     *
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Create the notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText("Service is running in the background")
                .setSmallIcon(R.drawable.ic_notification) // Ensure this drawable exists
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Make the notification persistent
                .build();

        // Start the service in the foreground
        startForeground(1, notification);

        log(TAG_ID, "Geogram is starting");

        // Start periodic logging
        handler = new Handler();
        logTask = new Runnable() {
            @Override
            public void run() {
                log(TAG_ID, "Service is running...");
                if(hasNecessaryPermissions){
                    runBackgroundTask();
                }else{
                    log(TAG_ID, "Missing permissions, cannot proceed");
                }
                handler.postDelayed(this, interval_seconds * 1000); // Repeat every NN seconds
            }
        };
        handler.post(logTask);

        // Start the Spark server in a background thread
        Thread serverThread = new Thread(new SimpleSparkServer());
        serverThread.start();

        log(TAG_ID, "Geogram was launched");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log(TAG_ID, "Service destroyed");

        // Stop periodic logging
        if (handler != null) {
            handler.removeCallbacks(logTask);
        }

        // stop bluetooth
        if(eddystoneBeacon != null){
            eddystoneBeacon.stopAdvertising();
            log(TAG_ID, "BLE beacon stopped");
        }


        // Stop Wi-Fi Direct advertising
        if (wifiDirectAdvertiser != null) {
            wifiDirectAdvertiser.stopAdvertising();
            log(TAG_ID, "Wi-Fi advertise stopped");
        }
//
//        // Stop Wi-Fi Direct discovery
//        if (discover != null) {
//            discover.stopDiscovery();
//            log(TAG_ID, "Wi-Fi discovery stopped");
//        }



    }

    /**
     * The repetitive function that runs every few seconds
     *  + Discover Wi-Fi direct peers
     *  + Synchronize messages and data with others
     */
    private void runBackgroundTask() {

        if(discover != null && wifi_discover) {
            discover.startDiscovery(1);
            discover.stopDiscovery();
            listPeers();
        }

    }

    private void listPeers() {
        if(peers == null){
            return;
        }
        for (WifiP2pDevice device : peers.getDeviceList()) {
            Log.i(TAG_ID, "Found P2P available: " + device.deviceName);
        }
    }

}
