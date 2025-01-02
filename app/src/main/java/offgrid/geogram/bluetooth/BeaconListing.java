package offgrid.geogram.bluetooth;

import static offgrid.geogram.bluetooth.BluetoothUtils.calculateDistance;
import static offgrid.geogram.util.DateUtils.getHumanReadableTime;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import offgrid.geogram.MainActivity;
import offgrid.geogram.R;
import offgrid.geogram.database.BeaconDatabase;
import offgrid.geogram.things.BeaconReachable;
import offgrid.geogram.core.Log;
import offgrid.geogram.fragments.BeaconDetailsFragment;

/**
 * Manages the list of beacons that were found
 * and lists them on the UI of the android app
 */
public class BeaconListing {

    private static final String TAG = "BeaconList";

    private static BeaconListing instance; // Singleton instance

    // list of beacons both reachable and past ones
    public final HashMap<String, BeaconReachable> beacons = new HashMap<>();

    // Private constructor for Singleton pattern
    private BeaconListing() {
        // Prevent instantiation
    }

    /**
     * Singleton access to the BeaconListing instance.
     */
    public static synchronized BeaconListing getInstance() {
        if (instance == null) {
            instance = new BeaconListing();
        }
        return instance;
    }

    /**
     * Updates the list of beacons on the UI.
     */
    public void updateList(Context context) {
        ListView beaconWindow = MainActivity.beacons;

        if (beaconWindow == null) {
            return;
        }

        // create a new list
        // add all beacons from the database
        HashMap<String, BeaconReachable> beaconsToList = new HashMap<>(BeaconDatabase.beacons);

        // now update with all the beacons within radio reach
        for(BeaconReachable beacon : BeaconFinder.getInstance(context).getBeaconMap().values()){
            // overwrite any existing items
            beaconsToList.put(beacon.getDeviceId(), beacon);
        }

        // transform into a simple arrayList
        ArrayList<BeaconReachable> beaconsList = new ArrayList<>(beaconsToList.values());
        // Sort beacons by last seen time, most recent first
        beaconsList.sort(Comparator.comparingLong(BeaconReachable::getTimeLastFound).reversed());

        ArrayList<String> displayList = new ArrayList<>();
        for (BeaconReachable beacon : beaconsList) {
            String distance = "Distance: " + calculateDistance(beacon.getRssi());
            long lastSeen = System.currentTimeMillis() - beacon.getTimeLastFound();

            if (lastSeen > 3 * 60_000) {
                distance = "Not reachable since " + getHumanReadableTime(beacon.getTimeLastFound());
            }

            String profileName = beacon.getProfileName();
            if(profileName == null){
                profileName = beacon.getDeviceId().substring(0, 6);
            }


            String text = profileName
                    + " ("
                    + beacon.getMacAddress()
                    + ")"
                    + "\n"
                    + distance;

            displayList.add(text);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                beaconWindow.getContext(),
                android.R.layout.simple_list_item_1,
                displayList
        );
        beaconWindow.setAdapter(adapter);

        // Add click listener to items
        beaconWindow.setOnItemClickListener((parent, view, position, id) -> {
            String selectedBeaconDetails = displayList.get(position);
            String deviceId = selectedBeaconDetails.substring(0, 6);
            BeaconReachable beacon = null;

            Collection<BeaconReachable> beacons = BeaconFinder.getInstance(context).getBeaconMap().values();
            for (BeaconReachable b : beacons) {
                if (b.getDeviceId().startsWith(deviceId)) {
                    beacon = b;
                    break;
                }
            }
            // cannot be null
            if (beacon == null) {
                Log.e(TAG, "Beacon not found: " + deviceId);
                return;
            }

            MainActivity.activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main, BeaconDetailsFragment.newInstance(beacon))
                    .addToBackStack(null)
                    .commit();
        });
    }


}
