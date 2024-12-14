package offgrid.geogram.wifi;

import static offgrid.geogram.MainActivity.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import offgrid.geogram.core.PermissionsHelper;

public class WiFiDirectDiscovery {

    public WifiP2pDeviceList devices = null;

    private static final String TAG = "WiFiDirectPeerDiscovery";

    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;
    private final Context context;
    private final BroadcastReceiver receiver;
    private boolean isReceiverRegistered = false;
    private boolean isDiscovering = false;

    public WiFiDirectDiscovery(Context context) {
        this.context = context;

        // Initialize the Wi-Fi P2P Manager
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);

        // Define a BroadcastReceiver to handle Wi-Fi P2P state changes
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                    if (!isDiscovering) {
                        try {
                            discoverPeers();
                        } catch (SecurityException e) {
                            Log.e(TAG, "Permission denied while discovering peers: " + e.getMessage());
                        }
                    }
                }
            }
        };
    }

    public void registerIntents() {
        if (!isReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
            context.registerReceiver(receiver, intentFilter);
            isReceiverRegistered = true;
        }
    }

    public void startDiscovery(int counter) {
        // Avoid endless loops
        if (counter > 3) {
            return;
        }

        // Register intents to ensure receiver is ready
        registerIntents();

        if (!hasPermissions()) {
            Log.e(TAG, "Missing necessary permissions. Repeating permission request");
            PermissionsHelper.requestPermissionsIfNecessary(activity);
            startDiscovery(counter + 1);
            return;
        }

        // Ensure Wi-Fi is enabled
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Log.e(TAG, "Wi-Fi is disabled. Prompting user to enable it.");
            // Prompt user to enable Wi-Fi
            Intent intent = new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK);
            context.startActivity(intent);
            return;
        }

        // Start peer discovery
        try {
            isDiscovering = true;
            wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Peer discovery started successfully");
                }

                @Override
                public void onFailure(int reason) {
                    isDiscovering = false;
                    switch (reason) {
                        case WifiP2pManager.P2P_UNSUPPORTED:
                            Log.e(TAG, "Peer discovery failed: P2P unsupported");
                            break;
                        case WifiP2pManager.BUSY:
                            Log.e(TAG, "Peer discovery failed: Framework busy");
                            break;
                        case WifiP2pManager.ERROR:
                        default:
                            Log.e(TAG, "Peer discovery failed: Internal error");
                            break;
                    }
                }
            });
        } catch (SecurityException e) {
            isDiscovering = false;
            Log.e(TAG, "Permission denied while starting peer discovery: " + e.getMessage());
        }
    }

    public void stopDiscovery() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(receiver);
                isReceiverRegistered = false;
                Log.i(TAG, "Peer discovery stopped.");
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Receiver not registered: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Receiver is not registered, cannot unregister.");
        }
    }

    private void discoverPeers() {
        if (!hasPermissions()) {
            Log.e(TAG, "Missing necessary permissions. Cannot discover peers.");
            return;
        }

        try {
            wifiP2pManager.requestPeers(channel, devices -> {
                for (WifiP2pDevice device : devices.getDeviceList()) {
                    Log.i(TAG, "Discovered peer: " + device.deviceName + " - " + device.deviceAddress);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied while requesting peers: " + e.getMessage());
        }
    }

    private boolean hasPermissions() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission not granted: ACCESS_FINE_LOCATION");
            return false;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission not granted: ACCESS_WIFI_STATE");
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission not granted: NEARBY_WIFI_DEVICES");
            return false;
        }
        return true;
    }
}
