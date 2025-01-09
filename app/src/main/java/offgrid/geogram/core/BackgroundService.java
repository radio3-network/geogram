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
import offgrid.geogram.bluetooth.other.DeviceListing;
import offgrid.geogram.bluetooth.BluetoothCentral;
import offgrid.geogram.server.SimpleSparkServer;
import offgrid.geogram.wifi.WiFiDirectAdvertiser;
import offgrid.geogram.wifi.WiFiDirectDiscovery;

public class BackgroundService extends Service {

    private static final String TAG_ID = "offgrid-service";
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private Handler handler;
    private Runnable logTask;

    private WiFiDirectAdvertiser wifiDirectAdvertiser; // Wi-Fi Direct Advertiser instance
    private WiFiDirectDiscovery wifiDiscover = null;

    // how long between scans
    private final long intervalSeconds = 10;

    private boolean wifiDiscoverEnabled = true;
    private boolean hasNecessaryPermissions = false;

    @Override
    public void onCreate() {
        super.onCreate();
        log(TAG_ID, "Geogram is starting");
        log(TAG_ID, "Creating the background service");

        // Load settings
        Central.getInstance().loadSettings(this.getApplicationContext());

        createNotificationChannel();
        initializePermissions();

        // Initialize Bluetooth services
        startBluetooth();

        // Start the web server
        Thread serverThread = new Thread(new SimpleSparkServer());
        serverThread.start();

        // Initialize periodic task
        handler = new Handler();
        logTask = new Runnable() {
            @Override
            public void run() {
                if (hasNecessaryPermissions) {
                    runBackgroundTask();
                } else {
                    log(TAG_ID, "Missing permissions, cannot proceed");
                }
                handler.postDelayed(this, intervalSeconds * 1000); // Repeat every interval
            }
        };
        handler.post(logTask);

        log(TAG_ID, "Geogram was launched");
    }

    private void createNotificationChannel() {
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
    }

    private void initializePermissions() {
        if (activity == null) {
            log(TAG_ID, "Activity is null, skipping permission initialization");
            return;
        }
        hasNecessaryPermissions = PermissionsHelper.requestPermissionsIfNecessary(activity);

        if (!hasNecessaryPermissions) {
            log(TAG_ID, "Permissions are not granted yet. Waiting for user response.");
            PermissionsHelper.requestPermissionsIfNecessary(activity);
        }
    }

    /**
     * Starts the Bluetooth beacon
     */
    private void startBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            log(TAG_ID, "Bluetooth is not supported on this device.");
            return;
        } else if (!bluetoothAdapter.isEnabled()) {
            log(TAG_ID, "Bluetooth is disabled. Please turn it on");
            return;
        }
        BluetoothCentral.getInstance(this).start();
    }

    private void startWiFiDiscover() {
        if (wifiDiscoverEnabled) {
            log(TAG_ID, "WiFi discovery initialized");
            wifiDiscover = new WiFiDirectDiscovery(this);
        } else {
            log(TAG_ID, "WiFi discovery disabled");
        }
    }

    private void startWiFiAdvertise() {
        boolean wifiAdvertiseEnabled = true;
        if (wifiAdvertiseEnabled) {
            wifiDirectAdvertiser = new WiFiDirectAdvertiser(this);
            wifiDirectAdvertiser.startAdvertising();
            log(TAG_ID, "WiFi advertise initialized");
        } else {
            log(TAG_ID, "WiFi advertise disabled");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Geogram Service")
                .setContentText("Service is running in the background")
                .setSmallIcon(R.drawable.ic_notification)
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

        if (handler != null) {
            handler.removeCallbacks(logTask);
        }

        BluetoothCentral.getInstance(this).stop();

        if (wifiDirectAdvertiser != null) {
            wifiDirectAdvertiser.stopAdvertising();
            log(TAG_ID, "Wi-Fi advertise stopped");
        }
    }

    private void runBackgroundTask() {
        if (wifiDiscover != null && wifiDiscoverEnabled) {
            wifiDiscover.startDiscovery(1);
            wifiDiscover.stopDiscovery();
            listPeers();
        }

        DeviceListing.getInstance().updateList(this.getApplicationContext());
    }

    private void listPeers() {
        if (peers == null) {
            return;
        }
        for (WifiP2pDevice device : peers.getDeviceList()) {
            log(TAG_ID, "Found P2P available: " + device.deviceName);
        }
    }
}
