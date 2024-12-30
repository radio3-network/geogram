package offgrid.geogram.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import offgrid.geogram.R;
import offgrid.geogram.bluetooth.BeaconFinder;
import offgrid.geogram.bluetooth.BluetoothUtils;
import offgrid.geogram.bluetooth.comms.Bluecomm;
import offgrid.geogram.core.Log;
import offgrid.geogram.things.BeaconReachable;
import offgrid.geogram.database.BeaconDatabase;
import offgrid.geogram.util.DateUtils;

public class BeaconDetailsFragment extends Fragment {

    private static final String TAG = "BeaconDetailsFragment";

    private static final String
            ARG_BEACON_DETAILS = "beacon_details";

    public static BeaconDetailsFragment newInstance(BeaconReachable beaconDiscovered)  {
        BeaconDetailsFragment fragment = new BeaconDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BEACON_DETAILS, beaconDiscovered.getDeviceId());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_beacon_details, container, false);


        // Handle back button
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Hide the floating action button
        FloatingActionButton btnAdd = requireActivity().findViewById(R.id.btn_add);
        if (btnAdd != null) {
            btnAdd.hide();
        }

        // Set beaconDiscovered details text
        TextView beaconDescription = view.findViewById(R.id.tv_beacon_description);
        TextView beaconDescriptionAdditional = view.findViewById(R.id.tv_beacon_additional_info);


        // we need to have valid arguments
        if (getArguments() == null) {
            Log.e(TAG, "Invalid arguments");
            return view;
        }

        // get the device Id
        String deviceId = getArguments().getString(ARG_BEACON_DETAILS);
        if (deviceId == null) {
            Log.e(TAG, "Invalid arguments: " + getArguments());
            return view;
        }

        // get the beacon from the list
        BeaconReachable beaconDiscovered =
                BeaconFinder.getInstance(this.getContext()).getBeaconMap().get(deviceId);
        if(beaconDiscovered == null){
            Log.e(TAG, "Beacon not found: " + deviceId);
            return view;
        }

        // this discovered beacon is already in our database?
        BeaconReachable beaconExisting = BeaconDatabase.getBeacon(beaconDiscovered.getDeviceId(), this.getContext());
        if(beaconExisting != null){
            beaconDiscovered.merge(beaconExisting);
        }

        // setup the title for this window
        String macAddress = beaconDiscovered.getMacAddress();
        String timeFirstFound = DateUtils.formatTimestamp(
                beaconDiscovered.getTimeFirstFound()
        );
        String timeLastFound = DateUtils.getHumanReadableTime(beaconDiscovered.getTimeLastFound());
        String rssi = String.valueOf(beaconDiscovered.getRssi());
        String distance = BluetoothUtils.calculateDistance(beaconDiscovered.getRssi());

        String text = "Device Id: " + deviceId
                + "\n"
                + "Address: " + macAddress
                + "\n"
                + "Distance: " + distance + " (RSSI = " + rssi + ")"
                + "\n"
                + "First seen: " + timeFirstFound
                + "\n"
                + "Last seen: " + timeLastFound;

        beaconDescription.setText("FlyingBarrel89" +
                "\n" +
                "On a mission to improve humankind");
        beaconDescriptionAdditional.setText(text);

        // Find the chat section
        View chatSection = view.findViewById(R.id.chat_section);

        // Set a click listener
        chatSection.setOnClickListener(v -> launchMessage(beaconDiscovered));

        return view;
    }

    private void launchMessage(BeaconReachable beaconDiscovered) {
        // Context from your activity or service
        Context context = this.getContext();
        Log.i("GetProfileExample", "Sending message to "
                + beaconDiscovered.getMacAddress());

        // MAC address of the Eddystone beacon you want to read data from
        String macAddress = beaconDiscovered.getMacAddress();

        // Create an instance of GetProfile
        Bluecomm getProfile = Bluecomm.getInstance(context);

        // Implement the callback
        Bluecomm.DataCallback callback = new Bluecomm.DataCallback() {
            @Override
            public void onDataSuccess(String data) {
                Log.i("GetProfileExample", "Data arrived: " + data);
            }

            @Override
            public void onDataError(String errorMessage) {
                Log.e("GetProfileExample", "Error sending data: " + errorMessage);
            }
        };

        // Attempt to read the data
        getProfile.writeData(macAddress, "test1", callback);
        Log.i("GetProfileExample", "Message sent");
        Toast.makeText(getContext(), "Message sent", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Ensure the floating action button is shown again when leaving the fragment
        FloatingActionButton btnAdd = requireActivity().findViewById(R.id.btn_add);
        if (btnAdd != null) {
            btnAdd.show();
        }
    }
}
