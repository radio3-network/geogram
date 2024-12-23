package offgrid.geogram.settings;

import android.Manifest;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import offgrid.geogram.R;

public class SettingsFragment extends Fragment {

    private static final int REQUEST_WRITE_PERMISSION = 100;
    SettingsUser settings = null;

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

        // Load settings
        settings = SettingsLoader.loadSettings(requireContext());

        // Privacy Options
        Switch listenOnlySwitch = view.findViewById(R.id.switch_listen_only);
        listenOnlySwitch.setChecked(settings.invisibleMode);

        listenOnlySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.invisibleMode = isChecked;
            Toast.makeText(getContext(), isChecked ? "Listen-only enabled" : "Listen-only disabled", Toast.LENGTH_SHORT).show();
        });

        // User Preferences
        EditText nickname = view.findViewById(R.id.edit_preferred_nickname);
        EditText bio = view.findViewById(R.id.edit_intro);
        nickname.setText(settings.nickname);
        bio.setText(settings.intro);

        // NOSTR Identity
        EditText npub = view.findViewById(R.id.edit_npub);
        EditText nsec = view.findViewById(R.id.edit_nsec);
        npub.setText(settings.npub);
        nsec.setText(settings.nsec);

        // Beacon Preferences
        Spinner beaconTypeSpinner = view.findViewById(R.id.spinner_beacon_type);
        EditText groupId = view.findViewById(R.id.edit_group_id);
        EditText deviceId = view.findViewById(R.id.edit_device_id);
        groupId.setText(settings.idGroup);
        deviceId.setText(settings.idDevice);

        // Populate the spinner with beacon types
        String[] beaconTypes = getResources().getStringArray(R.array.beacon_types);
        for (int i = 0; i < beaconTypes.length; i++) {
            if (beaconTypes[i].equals(settings.beaconType)) {
                beaconTypeSpinner.setSelection(i);
                break;
            }
        }

        // Floating Action Button for saving settings
        View saveButton = view.findViewById(R.id.btn_save_settings);
        saveButton.setOnClickListener(v -> {
            saveSettings(nickname, bio, npub, nsec, beaconTypeSpinner, groupId, deviceId, listenOnlySwitch.isChecked());
        });

        return view;
    }

    private void saveSettings(EditText nickname, EditText bio, EditText npub, EditText nsec,
                              Spinner beaconTypeSpinner, EditText groupId, EditText deviceId, boolean listenOnly) {

        // Update settings object
        settings.nickname = nickname.getText().toString();
        settings.intro = bio.getText().toString();
        settings.npub = npub.getText().toString();
        settings.nsec = nsec.getText().toString();
        settings.beaconType = beaconTypeSpinner.getSelectedItem().toString();
        settings.idGroup = groupId.getText().toString();
        settings.idDevice = deviceId.getText().toString();
        settings.invisibleMode = listenOnly;

        // Save to JSON file
        File file = new File(requireContext().getFilesDir(), "settings.json");
        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new Gson();
            writer.write(gson.toJson(settings));
            Toast.makeText(requireContext(), "Settings saved successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error saving settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Permission granted. Press save again.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Permission denied. Cannot save settings.", Toast.LENGTH_SHORT).show();
        }
    }
}
