package offgrid.geogram;

import static offgrid.geogram.core.Messages.log;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import offgrid.geogram.core.Art;
import offgrid.geogram.core.BackgroundService;
import offgrid.geogram.core.Log;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MainActivity";
    public static Activity activity = null;
    public static EditText logWindow = null;
    public static ListView beacons  = null;

    private TextView tvStatus;
    private Button btnDiscover;
    private ListView lvUsers;
    private Button btnGo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // setup the navigation
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_settings) {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
            } else if (item.getItemId() == R.id.nav_debug) {
                Toast.makeText(this, "Debug clicked", Toast.LENGTH_SHORT).show();
            } else if (item.getItemId() == R.id.nav_about) {
                Toast.makeText(this, "About clicked", Toast.LENGTH_SHORT).show();
            }

            // Close the drawer after an item is clicked
            DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
            drawerLayout.closeDrawer(GravityCompat.START);

            return true;
        });



        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ImageButton btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        //tvStatus = findViewById(R.id.tv_status);
        //btnGo = findViewById(R.id.btn_connect);
        beacons = findViewById(R.id.lv_beacons);
        logWindow = findViewById(R.id.lv_log);
        Log.setLogWindow(logWindow);

        // setup the debug spinner
        //Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.spinner_options, android.R.layout.simple_spinner_item
        );

//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner.setAdapter(adapter);

        // output the starter logo


        // Log UI initialization
        log("Geogram", Art.logo1());
        //addTextToLogWindow(TAG, "UI was launched");
        activity = this;

        // launch the background service
        startBackgroundService();

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


}
