package offgrid.geogram.core;

import static offgrid.geogram.core.Messages.log;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import offgrid.geogram.MainActivity;
import offgrid.geogram.R;
import offgrid.geogram.wifi.WiFiDirectAdvertiser;
import offgrid.geogram.wifi.WiFiDirectDiscovery;

public class BackgroundService extends Service {

    private static final String TAG_ID = "offgrid-service";
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private Handler handler;
    private Runnable logTask;

    private WiFiDirectAdvertiser wifiDirectAdvertiser; // Wi-Fi Direct Advertiser instance

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

        // Initialize Wi-Fi Direct Advertiser
        wifiDirectAdvertiser = new WiFiDirectAdvertiser(this);
        log(TAG_ID, "WiFiDirectAdvertiser initialized");
    }

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

        log(TAG_ID, "Offgrid service is starting");

        // Start Wi-Fi Direct advertising
        if (wifiDirectAdvertiser != null) {
            wifiDirectAdvertiser.startAdvertising();
            log(TAG_ID, "Wi-Fi Direct advertising started");
        }

        // Start periodic logging
        handler = new Handler();
        logTask = new Runnable() {
            @Override
            public void run() {
                log(TAG_ID, "Service is running...");
                runBackgroundTask();
                handler.postDelayed(this, 30_000); // Repeat every NN seconds
            }
        };
        handler.post(logTask);

        log(TAG_ID, "Offgrid service was launched");
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

        // Stop Wi-Fi Direct advertising
        if (wifiDirectAdvertiser != null) {
            wifiDirectAdvertiser.stopAdvertising();
            log(TAG_ID, "Wi-Fi Direct advertising stopped");
        }
    }

    /**
     * The repetitive function that runs every few seconds
     *  + Discover Wi-Fi direct peers
     *  + Synchronize messages and data with others
     */
    private void runBackgroundTask() {
        WiFiDirectDiscovery discover = new WiFiDirectDiscovery(this);
        discover.startDiscovery(1);
        discover.stopDiscovery();
    }

}
