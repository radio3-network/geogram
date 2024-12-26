package offgrid.geogram.bluetooth.broadcast;

/**
 * Stores a message that was broadcasted to all devices within reach
 */
public class BroadcastMessage {
    private final String message;
    private final String deviceId;
    private final long timestamp;

    public BroadcastMessage(String message, String deviceId) {
        this.message = message;
        this.deviceId = deviceId;
        this.timestamp = System.currentTimeMillis();
    }
}
