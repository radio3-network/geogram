package offgrid.geogram.core;

import static offgrid.geogram.MainActivity.activity;
import static offgrid.geogram.core.Messages.log;
import static offgrid.geogram.wifi.WiFiCommon.peers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import offgrid.geogram.MainActivity;
import offgrid.geogram.R;
import offgrid.geogram.bluetooth.BeaconListing;
import offgrid.geogram.bluetooth.BluetoothCentral;
import offgrid.geogram.database.BeaconDatabase;
import offgrid.geogram.server.SimpleSparkServer;
import offgrid.geogram.wifi.WiFiDirectAdvertiser;
import offgrid.geogram.wifi.WiFiDirectDiscovery;

public class BackgroundService extends Service {

    private static final String TAG_ID = "offgrid-service";
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private Handler handler;
    private Runnable logTask;

    private WiFiDirectAdvertiser wifiDirectAdvertiser; // Wi-Fi Direct Advertiser instance
    private WiFiDirectDiscovery WifiDiscover = null;

    // how long between scans
    private final long interval_seconds = 10;


    private boolean wifi_discover = true;
    private boolean hasNecessaryPermissions = false;


    @Override
    public void onCreate() {
        super.onCreate();
        log(TAG_ID, "Geogram is starting");
        log(TAG_ID, "Creating the background service");

        // load the settings
        Central.getInstance().loadSettings(this.getApplicationContext());

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

        // sometimes called in a weird way during processing
        if(activity == null){
            return;
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

        // initialize the bluetooth services
        startBluetooth();


        // start the web server
        Thread serverThread = new Thread(new SimpleSparkServer());
        serverThread.start();


        // Existing logic
        handler = new Handler();
        logTask = new Runnable() {
            @Override
            public void run() {
                if (hasNecessaryPermissions) {
                    runBackgroundTask();
                } else {
                    log(TAG_ID, "Missing permissions, cannot proceed");
                }
                handler.postDelayed(this, interval_seconds * 1000); // Repeat every interval
            }
        };
        handler.post(logTask);

        log(TAG_ID, "Geogram was launched");

    }

    /**
     * Starts the Bluetooth beacon
     * Notice that eddyStoneBeacon variable will no longer be null
     * when it is starting and running as expected.
     */
    private void startBluetooth() {
        // Check if Bluetooth is enabled
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            //Toast.makeText(this, "Bluetooth is not supported on this device.", Toast.LENGTH_LONG).show();
            return;
        } else if (!bluetoothAdapter.isEnabled()) {
            //Toast.makeText(this, "Bluetooth is disabled. Please turn it on", Toast.LENGTH_LONG).show();
            return;
        }

        // Initialize and start BluetoothCentral
        BluetoothCentral bluetoothCentral = BluetoothCentral.getInstance(this);
        bluetoothCentral.start();
    }



        /**
         * Starts the Wi-Fi Direct discovery
         */
    private void startWiFiDiscover() {
        if(wifi_discover) {
            log(TAG_ID, "WiFi discovery initialized");
            WifiDiscover = new WiFiDirectDiscovery(this);
            //discover.registerIntents();
            //discover.startDiscovery(1);
        }else{
            log(TAG_ID, "WiFi discover disabled");
        }
    }

    private void startWiFiAdvertise() {
        boolean wifi_advertise = true;
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
     * @return the type of start
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Ensure startForeground() is called immediately

            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            // Create a notification
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Geogram Service")
                    .setContentText("Service is running in the background")
                    .setSmallIcon(R.drawable.ic_notification) // Ensure this drawable exists
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();

            startForeground(1, notification);

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
        BluetoothCentral bluetoothCentral = BluetoothCentral.getInstance(this);
        bluetoothCentral.stop();

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

        if(WifiDiscover != null && wifi_discover) {
            WifiDiscover.startDiscovery(1);
            WifiDiscover.stopDiscovery();
            listPeers();
        }

        // update the beacons found on disk
        BeaconDatabase.updateBeacons(this.getApplicationContext());

        // update the beacons list on the UI
        BeaconListing.getInstance().updateList(this.getApplicationContext());

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
