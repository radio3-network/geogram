package offgrid.geogram.wifi;

import android.content.Context;

import java.util.HashMap;

import offgrid.geogram.wifi.details.WiFiNetwork;

/**
 * Singleton class for managing Wi-Fi database operations.
 */
public class WiFiDatabase {

    // List of currently reachable Wi-Fi hosts
    // String is the two-byte hash of the host SSID
    private final HashMap<String, WiFiNetwork> networksReachable = new HashMap<>();

    // Static instance of the singleton
    private static WiFiDatabase instance;

    // Stored context
    private final Context context;

    /**
     * Private constructor to prevent direct instantiation.
     *
     * @param context the application context
     */
    private WiFiDatabase(Context context) {
        this.context = context.getApplicationContext(); // Use Application Context to prevent memory leaks
    }

    /**
     * Returns the singleton instance of WiFiDatabase.
     *
     * @param context the application context
     * @return the singleton instance
     */
    public static synchronized WiFiDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new WiFiDatabase(context);
        }
        return instance;
    }

    /**
     * Adds a Wi-Fi host to the database.
     *
     * @param ssidHash SSID hash to be added
     * @param deviceId Device ID
     * @param ssid     SSID
     * @param password Password
     */
    public void addNetworkReachable(WiFiNetwork network) {
        if(network.isNotMinimallyComplete()){
            return;
        }
        if (networksReachable.containsKey(network.getSSIDhash())) {
            WiFiNetwork networkExisting = networksReachable.get(network.getSSIDhash());
            if (networkExisting != null) {
                networkExisting.setTimeLastSeen(System.currentTimeMillis());
                networkExisting.setPassword(network.getPassword());
                networkExisting.setTimeLastSeen(System.currentTimeMillis());
            }
            return;
        }
        // Place the host on the reachable list
        network.setTimeFirstSeen(System.currentTimeMillis());
        network.setTimeLastSeen(System.currentTimeMillis());
        networksReachable.put(network.getSSIDhash(), network);
    }

    /**
     * Gets a Wi-Fi host from the database.
     *
     * @param ssidHash the hash of the host SSID
     * @return the Wi-Fi host or null if not found
     */
    public WiFiNetwork getReachableNetwork(String ssidHash) {
        return networksReachable.get(ssidHash);
    }

    /**
     * Clears the list of reachable networks.
     */
    public void clearReachableNetworks() {
        networksReachable.clear();
    }

    /**
     * Gets the stored application context.
     *
     * @return the application context
     */
    public Context getContext() {
        return context;
    }

    public String getNumberOfNetworksReachable() {
        return String.valueOf(networksReachable.size());
    }
}
