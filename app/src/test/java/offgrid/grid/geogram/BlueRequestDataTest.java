package offgrid.grid.geogram;

import org.junit.Test;
import static org.junit.Assert.*;

import offgrid.geogram.bluetooth.comms.BlueRequestData;

public class BlueRequestDataTest {

    @Test
    public void testRandomId() {
        BlueRequestData sender = BlueRequestData.createSender("HelloWorldThisIsATest");
        String randomId = sender.generateRandomId();
        assertNotNull(randomId);
        assertEquals(2, randomId.length());
    }


    @Test
    public void testCreateSender() {
        BlueRequestData sender = BlueRequestData.createSender("HelloWorldThisIsATest");

        assertNotNull(sender);
        assertEquals("HelloWorldThisIsATest", sender.getData());
        assertEquals(15, sender.getTextLengthPerParcel());
        assertEquals(2, sender.getMessageParcelsTotal()); // Total parcels = ceil(20 / 15)
        assertTrue(sender.isTransferring());
    }

    @Test
    public void testReceiverReconstruction1() {

        BlueRequestData sender = BlueRequestData.createSender("HelloWorldThisIsATestThatGoesAroundAndShouldBreakToMultipleMessagesOK?");

        String parcel = sender.getNextParcel();
        String id = parcel.substring(0, 2);
        String data = parcel.substring(5);
        int parcelTotal = Integer.parseInt(data);
        assertEquals(5, parcelTotal);

        // second part
        String parcel1 = sender.getNextParcel();
        String parcel2 = sender.getNextParcel();

        BlueRequestData receiver = BlueRequestData.createReceiver("ab:003");

        receiver.receiveParcel("ab001:DataPart1");
        receiver.receiveParcel("ab003:DataPart3");
        receiver.receiveParcel("ab002:DataPart2");

        assertTrue(receiver.allParcelsReceivedAndValid());
        String dataReceived = receiver.getData();
        assertEquals("DataPart1DataPart2DataPart3", dataReceived);
    }



    @Test
    public void testReceiverReconstruction2() {

        BlueRequestData sender = BlueRequestData.createSender("HelloWorldThisIsATestThatGoesAroundAndShouldBreakToMultipleMessagesOK?");

        // we assume the first parcel as the header
        BlueRequestData receiver = null;
        String initialHeader = sender.getNextParcel();
        try {
            receiver = BlueRequestData.createReceiver(initialHeader);
        } catch (Exception e) {
            fail("Invalid header format");
        }

        for(int i = 0; i < receiver.getMessageParcelsTotal(); i++){
            String text = sender.getNextParcel();
            System.out.println(text);
            receiver.receiveParcel(text);
        }

        assertTrue(receiver.allParcelsReceivedAndValid());
        String dataReceived = receiver.getData();
        assertEquals("HelloWorldThisIsATestThatGoesAroundAndShouldBreakToMultipleMessagesOK?", dataReceived);


    }


//    @Test
//    public void testGetNextParcel() {
//        BlueRequestData sender = BlueRequestData.createSender("HelloWorldThisIsATest");
//
//        String firstParcel = sender.getNextParcel();
//        assertNotNull(firstParcel);
//        assertTrue(firstParcel.matches("[a-f0-9]{4}:002")); // Header format AA:BBB
//
//        String secondParcel = sender.getNextParcel();
//        assertNotNull(secondParcel);
//        assertTrue(secondParcel.matches("[a-f0-9]{4}001:.*")); // Data format AA###:parcelText
//    }
//
//    @Test
//    public void testReceiverReconstruction() {
//        BlueRequestData receiver = BlueRequestData.createReceiver("ab:003");
//
//        receiver.receiveParcel("ab001:DataPart1");
//        receiver.receiveParcel("ab003:DataPart3");
//        receiver.receiveParcel("ab002:DataPart2");
//
//        assertTrue(receiver.allParcelsReceived());
//        assertEquals("DataPart1DataPart2DataPart3", receiver.getData());
//    }
//
//    @Test
//    public void testPartialReceive() {
//        BlueRequestData receiver = BlueRequestData.createReceiver("cd:003");
//
//        receiver.receiveParcel("cd001:Part1");
//        receiver.receiveParcel("cd003:Part3");
//
//        assertFalse(receiver.allParcelsReceived());
//        assertNull(receiver.getData());
//    }
//
//    @Test
//    public void testInvalidParcelReception() {
//        BlueRequestData receiver = BlueRequestData.createReceiver("ef:002");
//
//        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
//            receiver.receiveParcel("zz001:Invalid");
//        });
//
//        assertEquals("Invalid parcel format or ID mismatch", exception.getMessage());
//    }
//
//    @Test
//    public void testSpecificParcelRequest() {
//        BlueRequestData sender = BlueRequestData.createSender("ParcelSpecificTest");
//        assertEquals("ParcelSpeci", sender.getSpecificParcel(1));
//        assertEquals("ficTest", sender.getSpecificParcel(2));
//        assertNull(sender.getSpecificParcel(3)); // Invalid parcel index
//    }
}
