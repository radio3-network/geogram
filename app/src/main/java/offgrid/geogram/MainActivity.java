package offgrid.geogram;

import static offgrid.geogram.core.Messages.log;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import offgrid.geogram.core.Art;
import offgrid.geogram.core.BackgroundService;
import offgrid.geogram.core.Log;
import offgrid.geogram.fragments.AboutFragment;
import offgrid.geogram.fragments.DebugFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static Activity activity = null;
    public static EditText logWindow = null;
    public static ListView beacons = null;

    private FloatingActionButton btnAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        btnAdd = findViewById(R.id.btn_add);
        beacons = findViewById(R.id.lv_beacons);
        logWindow = findViewById(R.id.lv_log);
        Log.setLogWindow(logWindow);

        // Handle window insets for modern devices
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup navigation drawer
        setupNavigationDrawer();

        // Handle floating action button
        setupBackPressedHandler();

        // Output starter logo to log window
        log("Geogram", Art.logo1());
        activity = this;

        // Launch background service
        startBackgroundService();
    }

    private void setupNavigationDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        // Open the drawer when settings button is clicked
        ImageButton btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Handle navigation item clicks using if-else conditions
        navigationView.setNavigationItemSelectedListener(item -> {
            FragmentManager fragmentManager = getSupportFragmentManager();

            if (item.getItemId() == R.id.nav_settings) {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
                btnAdd.show();
            } else if (item.getItemId() == R.id.nav_debug) {
                // Navigate to DebugFragment
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.main, new DebugFragment());
                transaction.addToBackStack(null);
                transaction.commit();
                btnAdd.hide(); // Hide FAB when viewing Debug
            } else if (item.getItemId() == R.id.nav_about) {
                // Navigate to AboutFragment
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.main, new AboutFragment());
                transaction.addToBackStack(null);
                transaction.commit();
                btnAdd.hide(); // Hide FAB when viewing About
            }

            // Close the drawer after an item is clicked
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupBackPressedHandler() {
        // Handle back press with OnBackPressedDispatcher
        this.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStack(); // Navigate back to the previous fragment
                } else {
                    finish(); // Exit the app if no fragments are in the back stack
                }

                // Ensure the Floating Action Button is visible when leaving About screen
                btnAdd.show();
            }
        });
    }

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
