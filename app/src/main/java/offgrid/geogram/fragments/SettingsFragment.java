package offgrid.geogram.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import offgrid.geogram.R;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Back button functionality
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Privacy Options
        Switch listenOnlySwitch = view.findViewById(R.id.switch_listen_only);
        listenOnlySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(getContext(), isChecked ? "Listen-only enabled" : "Listen-only disabled", Toast.LENGTH_SHORT).show();
        });

        // User Preferences
        EditText nickname = view.findViewById(R.id.edit_preferred_nickname);
        EditText bio = view.findViewById(R.id.edit_intro);

        // NOSTR Identity
        EditText npub = view.findViewById(R.id.edit_npub);
        EditText nsec = view.findViewById(R.id.edit_nsec);

        // Beacon Preferences
        Spinner beaconTypeSpinner = view.findViewById(R.id.spinner_beacon_type);
        EditText groupId = view.findViewById(R.id.edit_group_id);
        EditText deviceId = view.findViewById(R.id.edit_device_id);

        return view;
    }
}
