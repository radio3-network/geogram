package offgrid.geogram.bluetooth.broadcast;

import android.content.Context;

import java.util.ArrayList;

import offgrid.geogram.bluetooth.BluetoothCentral;
import offgrid.geogram.bluetooth.old.EddystoneBeacon;
import offgrid.geogram.bluetooth.old.BeaconManager;
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
    public static boolean broadcast(String message, Context context) {

        BluetoothCentral bluetoothCentral = BluetoothCentral.getInstance(context);
        if (bluetoothCentral.isBluetoothAvailable()) {
            // Bluetooth is available and enabled, proceed with operations
            bluetoothCentral.start();
        } else {
            // Bluetooth is either not supported or not enabled
            Log.i(TAG_ID, "Bluetooth is not ready. Please enable it to continue.");
            return false;
        }

        // send the message
        bluetoothCentral.broadcastMessageToAllEddystoneDevices(message);
        return true;
    }
}
