package offgrid.geogram.wifi.comm;

import offgrid.geogram.wifi.messages.Message;

/**
 * To communicate between WiFi devices we use this box.
 * It does not know what is inside, just knows that it
 * needs to deliver a message to someone.
 */

public class WiFiMessage {
    final CID CIDtype;
    final long timeStamp;
    final Message message;

    DeliveryPriority priority = DeliveryPriority.NORMAL;
    final String targetSSID;
    final String targetIP;

    // when did we last tried to send this package
    private long timeLastAttemptToSend = -1;
    private long timeToExpireFromSending = -1;

    private int attemptedToSend = 0;

    public WiFiMessage(String targetSSID, String targetIP, Message message) {
        this.CIDtype = message.getCid();
        this.timeStamp = message.getTimeStamp();
        this.targetIP = targetIP;
        this.targetSSID = targetSSID;
        this.message = message;
    }
}
