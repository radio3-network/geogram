package offgrid.geogram;

import static offgrid.geogram.core.Messages.log;
import static offgrid.geogram.core.Messages.message;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import offgrid.geogram.core.BackgroundService;
import offgrid.geogram.core.Messages;
import offgrid.geogram.core.PermissionsHelper;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MainActivity";
    public static Activity activity = null;

    private TextView tvStatus;
    private Button btnDiscover;
    private ListView lvPeers;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvStatus = findViewById(R.id.tv_status);
        lvPeers = findViewById(R.id.lv_log);
        btnConnect = findViewById(R.id.btn_connect);

        // Log UI initialization
        log(TAG, "UI was launched");
        activity = this;

        // Check and request permissions
        if (!PermissionsHelper.requestPermissionsIfNecessary(activity)) {
            log(TAG, "Permissions are not granted yet. Waiting for user response.");
        }

        // needs to have permissions by now
        if (PermissionsHelper.requestPermissionsIfNecessary(activity)) {
            startBackgroundService();
        }else{
            Messages.message(this, "Please enable the app permissions");
        }
    }

    /**
     * Starts the BackgroundService as a foreground service.
     */
    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            log(TAG, "Starting BackgroundService as a foreground service");
            startForegroundService(serviceIntent); // For Android 8.0+ (API 26)
        } else {
            log(TAG, "Starting BackgroundService as a normal service");
            startService(serviceIntent); // For pre-Android 8.0
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == PERMISSION_REQUEST_CODE) {
//            boolean allPermissionsGranted = true;
//            for (int i = 0; i < permissions.length; i++) {
//                if (grantResults.length == 0 || grantResults[i] != PackageManager.PERMISSION_GRANTED) {
//                    message(this, permissions[i] + " permission not granted");
//                    allPermissionsGranted = false;
//                }
//            }
//            if (allPermissionsGranted) {
//                // Permissions granted
//                Central.hasNeededPermissions = true;
//                message(this, "Needed permissions are enabled");
//                startBackgroundService(); // Start the service once permissions are granted
//            } else {
//                // Handle permission denial
//                message(this, "Failed: Necessary permissions denied.");
//            }
//        }
//    }
}
