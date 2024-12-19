package offgrid.geogram.bluetooth;

/**
 * Defines information about a beacon that was
 * found by this device
 */
public class Beacon {
    private String macAddress;
    private int rssi;
    private byte[] serviceData;
    private final long timeFirstFound;
    private long timeLastFound;

    public Beacon() {
        this.timeFirstFound = System.currentTimeMillis();
        this.timeLastFound = this.timeFirstFound;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public byte[] getServiceData() {
        return serviceData;
    }

    public void setServiceData(byte[] serviceData) {
        this.serviceData = serviceData;
    }

    public long getTimeFirstFound() {
        return timeFirstFound;
    }

    public long getTimeLastFound() {
        return timeLastFound;
    }

    public void setTimeLastFound(long timeLastFound) {
        this.timeLastFound = timeLastFound;
    }
}
