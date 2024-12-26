package offgrid.geogram.bluetooth.broadcast;

import java.util.ArrayList;

import offgrid.geogram.bluetooth.EddystoneBeacon;
import offgrid.geogram.bluetooth.EddystoneBeaconManager;
import offgrid.geogram.core.Log;

public class BroadcastChat {
    private static final String TAG_ID = "offgrid-broadcast";

    // messages that were broadcasted
    public static ArrayList<BroadcastMessage> messages = new ArrayList<>();

    /**
     * Tries to send a message to all Eddystone beacon devices in reach.
     * @param message a short text message
     * @return false when something went wrong (e.g. Bluetooth not available)
     */
    public static boolean broadcast(String message) {
        // get the running beacon
        EddystoneBeacon beacon = EddystoneBeaconManager.getInstance().getEddystoneBeacon();
        if (beacon == null) {
            Log.e(TAG_ID, "Eddystone beacon is not running");
            return false;
        }
        boolean result = beacon.broadcastMessage(message);
        return result;
    }
}
