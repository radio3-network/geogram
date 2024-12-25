package offgrid.geogram.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import offgrid.geogram.R;
import offgrid.geogram.bluetooth.Beacon;
import offgrid.geogram.bluetooth.BeaconList;

public class BeaconDetailsFragment extends Fragment {

    private static final String
            ARG_BEACON_DETAILS = "beacon_details",
            ARG_BEACON_POSITION = "beacon_position";

    public static BeaconDetailsFragment newInstance(String beaconDetails, int beaconPosition) {
        BeaconDetailsFragment fragment = new BeaconDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BEACON_DETAILS, beaconDetails);
        args.putInt(ARG_BEACON_POSITION, beaconPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_beacon_details, container, false);

        Beacon beacon = null;

        // Set beacon details text
        TextView contentTextView = view.findViewById(R.id.beacon_details_content);
        if (getArguments() != null) {
            String beaconDetails = getArguments().getString(ARG_BEACON_DETAILS);
            int position = getArguments().getInt(ARG_BEACON_POSITION);
            beacon = BeaconList.beacons.get(position);
            contentTextView.setText(
                    beaconDetails
                    + " -> "
                    + beacon.getMacAddress()
            );
        }

        // Handle back button
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Hide the floating action button
        FloatingActionButton btnAdd = requireActivity().findViewById(R.id.btn_add);
        if (btnAdd != null) {
            btnAdd.hide();
        }

        return view;
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
