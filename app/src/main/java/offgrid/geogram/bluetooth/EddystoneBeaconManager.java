package offgrid.geogram.bluetooth;

public class EddystoneBeaconManager {

    private static EddystoneBeaconManager instance;
    private EddystoneBeacon eddystoneBeacon;

    private EddystoneBeaconManager() {
    }

    public static synchronized EddystoneBeaconManager getInstance() {
        if (instance == null) {
            instance = new EddystoneBeaconManager();
        }
        return instance;
    }

    public void setEddystoneBeacon(EddystoneBeacon beacon) {
        this.eddystoneBeacon = beacon;
    }

    public EddystoneBeacon getEddystoneBeacon() {
        return eddystoneBeacon;
    }
}
