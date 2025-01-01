package offgrid.geogram.bluetooth.broadcast;

/**
 * Stores a message that was broadcast to all devices within reach
 */
public class BroadcastMessage {
    private final String message;
    private final String macAddress;
    private final long timestamp;
    private final boolean writtenByMe;
    private String deviceId = null;

    public BroadcastMessage(String message, String macAddress, boolean writtenByMe) {
        this.message = message;
        this.macAddress = macAddress;
        this.timestamp = System.currentTimeMillis();
        this.writtenByMe = writtenByMe;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    public String getMessage() {
        return message;
    }
    public String getMacAddress() {
        return macAddress;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public boolean isWrittenByMe() {
        return writtenByMe;
    }

}
