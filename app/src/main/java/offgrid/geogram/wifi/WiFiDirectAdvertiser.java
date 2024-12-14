package offgrid.geogram.wifi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class WiFiDirectAdvertiser {
    private static final String TAG = "WiFiDirectAdvertiser";

    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;
    private final Context context;
    private final BroadcastReceiver receiver;

    public WiFiDirectAdvertiser(Context context) {
        this.context = context;

        // Initialize the Wi-Fi P2P Manager
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);

        // Define a BroadcastReceiver to handle Wi-Fi P2P state changes
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "Permission ACCESS_FINE_LOCATION not granted. Skipping connection info.");
                        return;
                    }
                    wifiP2pManager.requestConnectionInfo(channel, info -> {
                        if (info.groupFormed && info.isGroupOwner) {
                            Log.i(TAG, "This device is the Group Owner");
                        } else if (info.groupFormed) {
                            Log.i(TAG, "This device is a Client");
                        }
                    });
                } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "Permission ACCESS_FINE_LOCATION not granted. Skipping peer request.");
                        return;
                    }
                    wifiP2pManager.requestPeers(channel, peers -> {
                        for (WifiP2pDevice device : peers.getDeviceList()) {
                            Log.i(TAG, "Found peer: " + device.deviceName);
                        }
                    });
                } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                    Log.i(TAG, "This device's Wi-Fi Direct state changed");
                }
            }
        };
    }

    /**
     * Checks if all necessary permissions are granted.
     */
    private boolean hasPermissions() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission not granted: ACCESS_FINE_LOCATION");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission not granted: ACCESS_WIFI_STATE");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission not granted: CHANGE_WIFI_STATE");
            return false;
        }

        Log.i(TAG, "All required permissions are granted.");
        return true;
    }

    /**
     * Starts advertising as a Wi-Fi Direct group owner.
     */
    public void startAdvertising() {
        if (!hasPermissions()) {
            Log.e(TAG, "Missing necessary permissions. Cannot start advertising.");
            return;
        }

        try {
            wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Wi-Fi Direct group created. This device is now the Group Owner.");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Failed to create Wi-Fi Direct group. Reason: " + reason);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while creating Wi-Fi Direct group: " + e.getMessage());
        }

        // Register the receiver to listen for Wi-Fi Direct events
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        context.registerReceiver(receiver, intentFilter);
        Log.i(TAG, "Wi-Fi Direct advertising started.");
    }

    /**
     * Stops advertising and removes the group.
     */
    public void stopAdvertising() {
        if (!hasPermissions()) {
            Log.e(TAG, "Missing necessary permissions. Cannot stop advertising.");
            return;
        }

        try {
            wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Wi-Fi Direct group removed.");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Failed to remove Wi-Fi Direct group. Reason: " + reason);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while removing Wi-Fi Direct group: " + e.getMessage());
        }

        // Unregister the receiver
        try {
            context.unregisterReceiver(receiver);
            Log.i(TAG, "Wi-Fi Direct advertising stopped.");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver was not registered: " + e.getMessage());
        }
    }
}
