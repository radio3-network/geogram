package offgrid.geogram.wifi;

import android.content.Context;

import offgrid.geogram.bluetooth.eddystone.EddystoneNamespaceGenerator;
import offgrid.geogram.core.Log;
import offgrid.geogram.database.HelloDatabase;
import offgrid.geogram.devices.DeviceReachable;
import offgrid.geogram.util.JsonUtils;
import offgrid.geogram.wifi.details.WiFiNetwork;
import offgrid.geogram.wifi.messages.MessageHello_v1;

public class WiFiUpdates {

    private static final String TAG = "WiFiUpdates";


    /**
     * Display the information we have about this device.
     * Always try first to show cached information and then
     * show information updated from the other device over Wi-Fi
     */
    public static MessageHello_v1 sayHello(DeviceReachable deviceDiscovered, Context context) {
        // get the namespace info
        String[] data = EddystoneNamespaceGenerator.extractNamespaceDetails(
                deviceDiscovered.getNamespaceId()
        );
        // needs to have valid data inside
        if(data[0] == null && data[1] == null){
            return null;
        }

        // basic info
        String ssidHash = data[0];
        String ssidPassword = data[1];
        String deviceId = deviceDiscovered.getDeviceId();

        // get a network matching the name hash
        WiFiNetwork networkReachable =
                WiFiDatabase.getInstance(context).getReachableNetwork(ssidHash);

        if(networkReachable == null){
            Log.e(TAG, "SSID not found for hash: " + ssidHash);
            return null;
        }

        try {
            Log.i(TAG, "Target device SSID: " + networkReachable.getSSID());
            Log.i(TAG, "Target device password: " + ssidPassword);

            // Save the current Wi-Fi connection
            WiFiDirectConnector.getInstance(context).saveCurrentConnection();

            // Connect to the Wi-Fi Direct hotspot
            boolean connected = WiFiDirectConnector.getInstance(context)
                    .connectToNetwork(networkReachable.getSSID(), ssidPassword);

            if (connected) {
                Log.i(TAG, "Connected to Wi-Fi Direct network: " + networkReachable.getSSID());
            } else {
                Log.e(TAG, "Failed to connect to Wi-Fi Direct network: " + networkReachable.getSSID());
                return null;
            }

            // Get the IP address of this device
            String addressIP = WiFiDirectConnector.getInstance(context).getCurrentIpAddress();
            Log.i(TAG, "IP address of this device: " + addressIP);

            // Get the DHCP server IP address
            String targetIP = WiFiDirectConnector.getInstance(context).getDhcpServerIpAddress();
            Log.i(TAG, "DHCP address: " + targetIP);

            // Perform necessary actions (e.g., web requests)
            Log.i(TAG, "Performing actions with the Wi-Fi Direct network...");

            // send the hello
            MessageHello_v1 hello = new MessageHello_v1();
            String text = JsonUtils.convertToJsonText(hello);
            // send the json to the local server
            String reply = WiFiRequestor
                    .getInstance(null)
                    .postJson("http://" +
                                    targetIP +
                                    ":5050",
                            text
                    );

            // Disconnect from the Wi-Fi Direct hotspot
            Log.i(TAG, "Disconnecting from Wi-Fi Direct hotspot...");
            WiFiDirectConnector.getInstance(context).disconnect();
            Thread.sleep(2000); // Wait for the group to be fully cleaned up

            // provide the received hello
            return receivedHello(reply, context);

        } catch (Exception e) {
            Log.e(TAG, "Error in network operations: " + e.getMessage());
            return null;
        }
    }

    private static MessageHello_v1 receivedHello(String reply, Context context) {
        // we expect a reply that cannot be null
        if(reply == null){
            Log.e(TAG, "Reply to hello was null");
            return null;
        }

        // convert to the other hello
        MessageHello_v1 replyMessage = JsonUtils.parseJson(reply, MessageHello_v1.class);

        Log.i(TAG, "Received hello reply: " + replyMessage.getBioProfile().getNpub());
        // write the hello to the database
        HelloDatabase.save(replyMessage, context);
        return replyMessage;
    }
}
