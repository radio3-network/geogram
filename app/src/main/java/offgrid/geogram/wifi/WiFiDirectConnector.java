package offgrid.geogram.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import offgrid.geogram.core.Log;

/**
 * Singleton class for connecting to a Wi-Fi Direct network using known SSID and password,
 * retrieving the peer's IP address, making HTTP requests, managing previous connections, and disconnecting.
 */
public class WiFiDirectConnector {

    private static final String TAG = "WiFiDirectConnector";

    private static WiFiDirectConnector instance;

    private final Context context;
    private ConnectivityManager.NetworkCallback networkCallback;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private String peerIpAddress;
    private String previousSsid = null;
    private int previousNetworkId = -1;

    private WiFiDirectConnector(Context context) {
        this.context = context.getApplicationContext();
        initializeWifiP2p();
    }

    /**
     * Returns the singleton instance of WiFiDirectConnector.
     *
     * @param context the application context
     * @return the singleton instance
     */
    public static synchronized WiFiDirectConnector getInstance(Context context) {
        if (instance == null) {
            instance = new WiFiDirectConnector(context);
        }
        return instance;
    }

    /**
     * Initializes Wi-Fi P2P components.
     */
    private void initializeWifiP2p() {
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (wifiP2pManager != null) {
            channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
        }
    }

    /**
     * Saves the current Wi-Fi connection (SSID and Network ID).
     */
    public void saveCurrentConnection() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getNetworkId() != -1) {
                previousSsid = wifiInfo.getSSID().replace("\"", ""); // Remove quotes
                previousNetworkId = wifiInfo.getNetworkId();
                Log.i(TAG, "Saved current connection: SSID=" + previousSsid + ", Network ID=" + previousNetworkId);
            } else {
                Log.e(TAG, "No current Wi-Fi connection to save.");
            }
        }
    }

    /**
     * Reconnects to the previously saved Wi-Fi connection.
     *
     * @return true if reconnection was successful, false otherwise
     */
    public boolean reconnectToPreviousNetwork() {
        if (previousNetworkId == -1) {
            Log.e(TAG, "No previously saved network to reconnect to.");
            return false;
        }

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            boolean success = wifiManager.enableNetwork(previousNetworkId, true);
            if (success) {
                Log.i(TAG, "Reconnected to the previous network: SSID=" + previousSsid);
            } else {
                Log.e(TAG, "Failed to reconnect to the previous network: SSID=" + previousSsid);
            }
            return success;
        }
        return false;
    }

    /**
     * Retrieves the current IP address of the device when connected to a Wi-Fi network.
     *
     * @return the current IP address as a string, or null if unavailable
     */
    public String getCurrentIpAddress() {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    int ipAddress = wifiInfo.getIpAddress();
                    // Convert the packed integer to an IPv4 address
                    return InetAddress.getByAddress(new byte[]{
                            (byte) (ipAddress & 0xFF),
                            (byte) ((ipAddress >> 8) & 0xFF),
                            (byte) ((ipAddress >> 16) & 0xFF),
                            (byte) ((ipAddress >> 24) & 0xFF)
                    }).getHostAddress();
                }
            }
        } catch (Exception e) {
            Log.e("IPUtils", "Error retrieving IP address: " + e.getMessage());
        }
        return null; // Return null if unable to retrieve IP
    }


    /**
     * Retrieves the DHCP server IP address of the current Wi-Fi connection.
     *
     * @return the DHCP server IP address as a string, or null if unavailable
     */
    public String getDhcpServerIpAddress() {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                if (dhcpInfo != null) {
                    // Convert the integer IP address to a human-readable format
                    return InetAddress.getByAddress(new byte[]{
                            (byte) (dhcpInfo.serverAddress & 0xFF),
                            (byte) ((dhcpInfo.serverAddress >> 8) & 0xFF),
                            (byte) ((dhcpInfo.serverAddress >> 16) & 0xFF),
                            (byte) ((dhcpInfo.serverAddress >> 24) & 0xFF)
                    }).getHostAddress();
                }
            }
        } catch (Exception e) {
            Log.e("IPUtils", "Error retrieving DHCP server IP address: " + e.getMessage());
        }
        return null; // Return null if unable to retrieve DHCP server IP
    }

    /**
     * Retrieves the peer device's IP address.
     *
     * @return the peer's IP address as a string, or null if unavailable
     */
    public String getPeerIpAddress() {
        if (wifiP2pManager == null || channel == null) {
            Log.e(TAG, "Wi-Fi P2P is not initialized.");
            return null;
        }

        CountDownLatch latch = new CountDownLatch(1);
        wifiP2pManager.requestConnectionInfo(channel, info -> {
            if (info.groupFormed) {
                peerIpAddress = info.groupOwnerAddress.getHostAddress();
                Log.i(TAG, "Peer IP Address: " + peerIpAddress);
            } else {
                Log.e(TAG, "No group formed. Unable to get peer IP address.");
                peerIpAddress = null;
            }
            latch.countDown();
        });

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for peer IP: " + e.getMessage());
        }

        return peerIpAddress;
    }

    /**
     * Connects to a known Wi-Fi Direct network.
     *
     * @param ssid     the SSID of the Wi-Fi Direct network
     * @param password the password of the Wi-Fi Direct network
     * @return true if the connection was successful, false otherwise
     */
    public boolean connectToNetwork(String ssid, String password) {
        saveCurrentConnection(); // Save the current connection before switching
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return connectWithNetworkSpecifier(ssid, password);
        } else {
            return connectWithWifiConfiguration(ssid, password);
        }
    }

    private boolean connectWithNetworkSpecifier(String ssid, String password) {
        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build();

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.i(TAG, "Connected to Wi-Fi Direct network: " + ssid);
                result[0] = true;
                latch.countDown();
            }

            @Override
            public void onUnavailable() {
                Log.e(TAG, "Failed to connect to Wi-Fi Direct network: " + ssid);
                result[0] = false;
                latch.countDown();
            }
        };

        connectivityManager.requestNetwork(request, networkCallback);

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Connection interrupted: " + e.getMessage());
        }

        return result[0];
    }

    private boolean connectWithWifiConfiguration(String ssid, String password) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";
        config.preSharedKey = "\"" + password + "\"";
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int networkId = wifiManager.addNetwork(config);

        if (networkId != -1) {
            wifiManager.disconnect();
            wifiManager.enableNetwork(networkId, true);
            wifiManager.reconnect();
            Log.i(TAG, "Connected to Wi-Fi Direct network: " + ssid);
            return true;
        } else {
            Log.e(TAG, "Failed to configure Wi-Fi Direct network: " + ssid);
            return false;
        }
    }

    /**
     * Fetches a web page from the peer device.
     *
     * @param ipAddress the IP address of the peer device
     * @param port      the port number on which the web server is running
     * @param path      the path of the resource to fetch
     */
    public void fetchWebPage(String ipAddress, int port, String path) {
        //new Thread(() -> {
            try {
                String urlString = "http://" + ipAddress + ":" + port + "/" + path;
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                Log.i(TAG, "Fetching web page: " + urlString);
                int responseCode = connection.getResponseCode();
                Log.i(TAG, "Response Code: " + responseCode);

                if (responseCode == 200) { // HTTP OK
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    Log.i(TAG, "Response: " + response.toString());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching web page: " + e.getMessage());
            }
       // }).start();
    }

    /**
     * Disconnects from the Wi-Fi Direct network and releases the resources.
     */
    public void disconnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
                Log.i(TAG, "Disconnected from Wi-Fi Direct network.");
            }
        } else {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            wifiManager.disconnect();
            Log.i(TAG, "Disconnected from Wi-Fi Direct network.");
        }
    }
}
