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
        String packageId = data[0].substring(0, 2);
        String parcelNumber = data[0].substring(2);

        // ask for the whole package to be sent again
        BroadcastSendMessage.sendParcelToDevice(macAddress, Bluecomm.gapREPEAT, context);
    }

    /**
     * Checks if there is a missing gap on the parcels being received.
     * Whenever one is detected as missing, will ask for it again.
     * @param writeAction the package that is being received
     * @param macAddress the sender of this package
     * @param context useful for transmitting data
     */
    public static boolean hasMissingParcels(BluePackage writeAction, String macAddress, Context context) {
        // no gaps detected? Nothing to be done here
        if(writeAction.hasGaps() == false){
            return false;
        }
        // get the first gap that is missing
        String gapIndex = writeAction.getFirstGapParcel();
        // send this request back to the other device
        BroadcastSendMessage.sendParcelToDevice(macAddress, gapIndex, context);
        // there are still gaps, don't let this continue
        return true;
    }
}