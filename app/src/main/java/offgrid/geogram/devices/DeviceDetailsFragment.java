package offgrid.geogram.devices;

import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import offgrid.geogram.R;
import offgrid.geogram.bluetooth.eddystone.EddystoneNamespaceGenerator;
import offgrid.geogram.bluetooth.other.DeviceFinder;
import offgrid.geogram.bluetooth.other.comms.BlueDataWriteAndReadToOutside;
import offgrid.geogram.bluetooth.other.comms.DataCallbackTemplate;
import offgrid.geogram.core.Log;
import offgrid.geogram.database.BioDatabase;
import offgrid.geogram.database.BioProfile;
import offgrid.geogram.bluetooth.other.comms.DataType;
import offgrid.geogram.wifi.WiFiDatabase;
import offgrid.geogram.wifi.WiFiDirectConnector;
import offgrid.geogram.wifi.WiFiRequestor;
import offgrid.geogram.wifi.details.WiFiNetwork;

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
        TextView deviceDescription = view.findViewById(R.id.tv_device_description);
        TextView deviceDescriptionAdditional = view.findViewById(R.id.tv_device_additional_info);


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
        DeviceReachable deviceDiscovered =
                DeviceFinder.getInstance(this.getContext()).getDeviceMap().get(deviceId);
        if(deviceDiscovered == null){
            Log.e(TAG, "Device not found: " + deviceId);
            return view;
        }

        // bio can be obtained by bluetooth or wi-fi
        getDataFromWiFi(deviceDiscovered);



        BioProfile profile = BioDatabase.get(deviceId, this.getContext());
        if(profile == null){
            Log.i(TAG, "No bio data found for " + deviceId);
            return view;
        }

        deviceDescription.setText(profile.getNick());
        deviceDescriptionAdditional.setText("Waiting to receive more data about this device..");


        disableIcon(view, R.id.section_chat);
        disableIcon(view, R.id.section_messages);
        disableIcon(view, R.id.section_collections);
        disableIcon(view, R.id.section_stats);
        disableIcon(view, R.id.section_settings);



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
//        deviceDescription.setText("FlyingBarrel89" +
//                "\n" +
//                "On a mission to improve humankind");
//        deviceDescriptionAdditional.setText(text);

        // Find the chat section
        View chatSection = view.findViewById(R.id.section_chat);

        // Set a click listener
        chatSection.setOnClickListener(v -> launchMessage(deviceDiscovered));

        return view;
    }

    private void getDataFromWiFi(DeviceReachable deviceDiscovered) {
        // get the namespace info
        String[] data = EddystoneNamespaceGenerator.extractNamespaceDetails(
                deviceDiscovered.getNamespaceId()
        );
        // needs to have valid data inside
        if(data[0] == null && data[1] == null){
            return;
        }

        // basic info
        String ssidHash = data[0];
        String ssidPassword = data[1];
        String deviceId = deviceDiscovered.getDeviceId();

        WiFiNetwork networkReachable =
                WiFiDatabase.getInstance(this.getContext()).getReachableNetwork(ssidHash);

        if(networkReachable == null){
            Log.e(TAG, "SSID not found for hash: " + ssidHash);
            return;
        }

        new Thread(() -> {
            try {
                Log.i(TAG, "Target device SSID: " + networkReachable.getSSID());
                Log.i(TAG, "Target device password: " + ssidPassword);

                // Save the current Wi-Fi connection
                WiFiDirectConnector.getInstance(this.getContext()).saveCurrentConnection();

                // Connect to the Wi-Fi Direct hotspot
                boolean connected = WiFiDirectConnector.getInstance(this.getContext())
                        .connectToNetwork(networkReachable.getSSID(), ssidPassword);

                if (connected) {
                    Log.i(TAG, "Connected to Wi-Fi Direct network: " + networkReachable.getSSID());
                } else {
                    Log.e(TAG, "Failed to connect to Wi-Fi Direct network: " + networkReachable.getSSID());
                    return;
                }

                // Get the IP address of this device
                String addressIP = WiFiDirectConnector.getInstance(this.getContext()).getCurrentIpAddress();
                Log.i(TAG, "IP address of this device: " + addressIP);

                // Get the DHCP server IP address
                String dhcpIP = WiFiDirectConnector.getInstance(this.getContext()).getDhcpServerIpAddress();
                Log.i(TAG, "DHCP address: " + dhcpIP);

                // Perform necessary actions (e.g., web requests)
                Log.i(TAG, "Performing actions with the Wi-Fi Direct network...");
                //Thread.sleep(2000); // Simulate task delay


                // Example usage of WiFiRequestor
                WiFiRequestor requestor = WiFiRequestor.getInstance(getContext());
                String response = requestor.getPage("http://" +
                        dhcpIP +
                        ":5050");

                if (response != null) {
                    Log.i("Test", "Response: " + response);
                } else {
                    Log.e("Test", "Failed to fetch the page.");
                }



                // Disconnect from the Wi-Fi Direct hotspot
                Log.i(TAG, "Disconnecting from Wi-Fi Direct hotspot...");
                WiFiDirectConnector.getInstance(this.getContext()).disconnect();
                Thread.sleep(2000); // Wait for the group to be fully cleaned up

//                Log.i(TAG, "Reconnecting to original router...");
//                boolean reconnected = WiFiDirectConnector.getInstance(this.getContext())
//                        .connectToNetwork("---___---", "vodafone");
//
//                if (reconnected) {
//                    Log.i(TAG, "Successfully reconnected to router: ---___---");
//                } else {
//                    Log.e(TAG, "Failed to reconnect to router: ---___---");
//                }

                // Group SSID: DIRECT-Uj-TANK2
                // Group Passphrase: p3IWg01x
                // IP Address: 192.168.49.1:5050

            } catch (Exception e) {
                Log.e(TAG, "Error in network operations: " + e.getMessage());
            }
        }).start();


        // stop the connection
        //WiFiDirectConnector.getInstance(this.getContext()).disconnect();

//        new Thread(() -> {
//            // get the IP address of the other device
//            String addressIP = WiFiDirectConnector.getInstance(getContext()).getPeerIpAddress();
//
//            if (addressIP == null) {
//                Log.e(TAG, "Failed to get IP address of the other device");
//                return;
//            }
//
//            Log.i(TAG, "IP address of the other device: " + addressIP);
//
//            // test getting a web page
//            WiFiDirectConnector.getInstance(getContext()).fetchWebPage(addressIP, 5050, "/ask?text=Hello");
//
//        }).start();


    }

    private void disableIcon(View view, int value) {
        LinearLayout icons = view.findViewById(value);
        icons.setAlpha(0.2f); // Set transparency to 50%
        icons.setClickable(false); // Disable click events
    }

    private void launchMessage(DeviceReachable beaconDiscovered) {

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
