package offgrid.geogram.fragments;

import android.os.Bundle;
import android.os.Looper;
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
import offgrid.geogram.bluetooth.comms.BlueDataWriteAndReadToOutside;
import offgrid.geogram.bluetooth.comms.DataCallbackTemplate;
import offgrid.geogram.core.Log;
import offgrid.geogram.database.BioProfile;
import offgrid.geogram.things.BeaconReachable;
import offgrid.geogram.bluetooth.comms.DataType;

public class DeviceDetailsFragment extends Fragment {

    private static final String TAG = "DeviceDetailsFragment";

    private static final String
            ARG_BEACON_DETAILS = "beacon_details";

    public static DeviceDetailsFragment newInstance(BioProfile profile)  {
        DeviceDetailsFragment fragment = new DeviceDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BEACON_DETAILS, profile.getDeviceId());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_details, container, false);


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
//        BeaconReachable beaconExisting = BeaconDatabase.getBeacon(beaconDiscovered.getDeviceId(), this.getContext());
//        if(beaconExisting != null){
//            beaconDiscovered.merge(beaconExisting);
//        }
//
//        // setup the title for this window
//        String macAddress = beaconDiscovered.getMacAddress();
//        String timeFirstFound = DateUtils.formatTimestamp(
//                beaconDiscovered.getTimeFirstFound()
//        );
//        String timeLastFound = DateUtils.getHumanReadableTime(beaconDiscovered.getTimeLastFound());
//        String rssi = String.valueOf(beaconDiscovered.getRssi());
//        String distance = BluetoothUtils.calculateDistance(beaconDiscovered.getRssi());
//
//        String text = "Device Id: " + deviceId
//                + "\n"
//                + "Address: " + macAddress
//                + "\n"
//                + "Distance: " + distance + " (RSSI = " + rssi + ")"
//                + "\n"
//                + "First seen: " + timeFirstFound
//                + "\n"
//                + "Last seen: " + timeLastFound;
//
//        beaconDescription.setText("FlyingBarrel89" +
//                "\n" +
//                "On a mission to improve humankind");
//        beaconDescriptionAdditional.setText(text);

        // Find the chat section
        View chatSection = view.findViewById(R.id.chat_section);

        // Set a click listener
        chatSection.setOnClickListener(v -> launchMessage(beaconDiscovered));

        return view;
    }

    private void launchMessage(BeaconReachable beaconDiscovered) {

        // Implement the callback
        DataCallbackTemplate callback = new DataCallbackTemplate() {
            @Override
            public void onDataSuccess(String data){
                Log.i("GetUserFromDevice", "Data arrived: " + data);
                Looper.prepare();
                Toast.makeText(getContext(), data, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDataError(String errorMessage) {
                Log.e("GetUserFromDevice", "Error sending data: " + errorMessage);
            }
        };

        // setup a new request
        BlueDataWriteAndReadToOutside request = new BlueDataWriteAndReadToOutside();
        // MAC address of the Eddystone beacon you want to read data from
        String macAddress = beaconDiscovered.getMacAddress();
        request.setMacAddress(macAddress);
        callback.setMacAddress(macAddress);
        callback.setDeviceId(beaconDiscovered.getDeviceId());
        // what we are requesting as data to the device
        request.setRequest(DataType.G);
        // setup the callback
        request.setCallback(callback);
        // send the request
        request.send(this.getContext());
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
