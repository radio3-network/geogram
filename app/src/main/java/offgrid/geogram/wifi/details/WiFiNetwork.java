package offgrid.geogram.wifi.details;

/**
 * Documents the networks reachable at some point
 * It does not mean that they are reachable
 */
public class WiFiNetwork {
    private String SSIDhash;
    private String SSID;
    private String BSSID;
    private String capabilities;
    private String password;
    private Coordinates positionApproximate;
    private long timeFirstSeen;
    private long timeLastSeen;
    private WiFiType type = WiFiType.UNKNOWN;

    public String getSSIDhash() {
        return SSIDhash;
    }

    public void setSSIDhash(String SSIDhash) {
        this.SSIDhash = SSIDhash;
    }

    public String getSSID() {
        return SSID;
    }

    public void setSSID(String SSID) {
        this.SSID = SSID;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Coordinates getPositionApproximate() {
        return positionApproximate;
    }

    public void setPositionApproximate(Coordinates positionApproximate) {
        this.positionApproximate = positionApproximate;
    }

    public long getTimeFirstSeen() {
        return timeFirstSeen;
    }

    public void setTimeFirstSeen(long timeFirstSeen) {
        this.timeFirstSeen = timeFirstSeen;
    }

    public long getTimeLastSeen() {
        return timeLastSeen;
    }

    public void setTimeLastSeen(long timeLastSeen) {
        this.timeLastSeen = timeLastSeen;
    }

    public WiFiType getType() {
        return type;
    }

    public void setType(WiFiType type) {
        this.type = type;
    }

    public String getBSSID() {
        return BSSID;
    }

    public void setBSSID(String BSSID) {
        this.BSSID = BSSID;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    public boolean isNotMinimallyComplete() {
        if(this.SSID == null || this.SSID.isEmpty()){
            return false;
        }
        return true;
    }
}
