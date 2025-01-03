package offgrid.geogram.bluetooth.comms;

/**
 * This is used when writing bluetooth messages.
 * Each message is placed on a queue waiting for
 * the most suited time to be sent.
 */
public class BlueQueueItem {
    private final String macAddress;
    private final String data;
    private final long timestamp;

    public BlueQueueItem(String macAddress, String data) {
        this.macAddress = macAddress;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
