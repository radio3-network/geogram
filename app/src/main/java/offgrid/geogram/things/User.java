package offgrid.geogram.things;

import java.util.ArrayList;

/**
 * Defines a user which is normally a single person.
 * This single person can have several devices
 */
public class User {

    // the NPUBs that are associated to this user
    private ArrayList<NPUB> npubs = new ArrayList<>();

    // the devices that are associated to this user
    private ArrayList<BeaconReachable> devices = new ArrayList<>();


}
