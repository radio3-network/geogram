package offgrid.geogram.bluetooth.broadcast;

import android.content.Context;

import offgrid.geogram.bluetooth.comms.BluePackage;
import offgrid.geogram.bluetooth.comms.Bluecomm;
import offgrid.geogram.core.Log;

/**
 * Takes care of lost and found packages, asks for
 * them to be sent again
 */
public class LostAndFound {

    private static final String TAG = "LostAndFound";
    public static final String
            gapBroadcast = ">B:", // means a one line statement
            gapREPEAT = "REPEAT" // please send the whole package again
                    ;

    /**
     * We received a parcel section that wasn't the initial one.
     * This means we need to ask the sending device to be patient
     * and send it once again.
     * E.g. this is what we received: MH004:one
     * @param receivedData whatever was received
     * @param macAddress who is sending this to us
     * @param context to access files when needed
     */
    public static void decodeLostPackage(String receivedData, String macAddress, Context context) {
        // check the cases that we can handle here
        if(receivedData == null || receivedData.contains(":") == false){
            Log.i(TAG, "This isn't a packet that I can recover yet: " + receivedData);
            return;
        }
        String[] data = receivedData.split(":");
        int sizeOfHeader = data[0].length();
        if(sizeOfHeader != 5){
            Log.e(TAG, "Invalid header received, can't do much: " + receivedData);
            return;
        }
        // get the package id
        String packageId = data[0].substring(0, 2);
        // ask for the whole package to be sent again
        BroadcastSendMessage.sendParcelToDevice(macAddress, gapREPEAT
                + ":" +packageId, context);
        Log.i(TAG, "Lost package detected, requesting to be sent again: " + receivedData);
    }

    /**
     * Checks if there is a missing gap on the parcels being received.
     * Whenever one is detected as missing, will ask for it again.
     * @param packageIncomplete the package that is being received
     * @param macAddress the sender of this package
     * @param context useful for transmitting data
     */
    public static boolean hasMissingParcels(BluePackage packageIncomplete, String macAddress, Context context) {
        // no gaps detected? Nothing to be done here
        if(packageIncomplete.hasGaps() == false){
            return false;
        }

        String packageId = packageIncomplete.getId();
        BroadcastSendMessage.sendParcelToDevice(macAddress, gapREPEAT
                + ":" +packageId, context);
        Log.i(TAG, "Lost package detected, requesting to be sent again: " + packageId);
//        // get the first gap that is missing
//        String gapIndex = packageIncomplete.getFirstGapParcel();
//        // send this request back to the other device
//        BroadcastSendMessage.sendParcelToDevice(macAddress, gapIndex, context);
//        // there are still gaps, don't let this continue
        return true;
    }
}
