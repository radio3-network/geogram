package offgrid.grid.geogram.wifi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import offgrid.geogram.core.Central;
import offgrid.geogram.settings.SettingsLoader;
import offgrid.geogram.settings.SettingsUser;
import offgrid.geogram.wifi.comm.WiFiReceiver;
import offgrid.geogram.wifi.comm.WiFiSender;
import offgrid.geogram.wifi.comm.WiFiSenderDelete;
import offgrid.geogram.wifi.messages.MessageHello_v1;
/*
    Focus on the communication between two devices
    using Wi-Fi communication
 */

public class WiFiProtocolTest {

    @Test
    public void testHello() {

        /*
            The first command on the protocol is "Hello".
            Android A will connect to the hotspot on the Android B
            and request information about Android B.

            This information includes the public key, the device ID
            and an index of the collections inside the device.
            It also includes number of messages and other statistics
            that might be relevant for Android A to consider

         */

        SettingsUser settings = SettingsLoader.createDefaultSettings(null);
        Central.getInstance().setSettings(settings);

        WiFiReceiver receiver = new WiFiReceiver();
        WiFiSender sender = WiFiSender.getInstance(null);

        // create basic info
        String targetSSID = "TestNetwork";
        String targetIP = "192.168.49.1";


        // create a basic Hello message
        MessageHello_v1 hello = new MessageHello_v1();
        // send that message (place in the sending queue)
        sender.send(targetSSID, targetIP, hello);

        // make sure our sender receives the messages
        assertNotNull(hello);
        assertEquals(1, sender.getMessages().size());

        // run the local version of the receiver
        receiver.syncLocally();

        // make sure we have messages received locally
        assertEquals(1, WiFiReceiver.messagesReceived.size());



    }



}
