package offgrid.geogram.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    private SettingsUser settings;

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
        loadSettings();

        // Initialize UI components and bind settings
        initializeUI(view);

        return view;
    }

    private void loadSettings() {
        try {
            settings = SettingsLoader.loadSettings(requireContext());
        } catch (Exception e) {
            settings = new SettingsUser(); // Default settings if loading fails
            this.saveSettings(settings);
            Toast.makeText(getContext(), "Failed to load settings. Using defaults.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeUI(View view) {
        // Privacy Options
        Switch listenOnlySwitch = view.findViewById(R.id.switch_listen_only);
        listenOnlySwitch.setChecked(settings.isInvisibleMode());
        listenOnlySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> settings.setInvisibleMode(isChecked));

        // User Preferences
        EditText nickname = view.findViewById(R.id.edit_preferred_nickname);
        EditText intro = view.findViewById(R.id.edit_intro);
        nickname.setText(settings.getNickname());
        intro.setText(settings.getIntro());

        Spinner preferredColorSpinner = view.findViewById(R.id.spinner_preferred_color);
        String[] colorOptions = getResources().getStringArray(R.array.color_options);
        for (int i = 0; i < colorOptions.length; i++) {
            if (colorOptions[i].equals(settings.getPreferredColor())) {
                preferredColorSpinner.setSelection(i);
                break;
            }
        }

        // NOSTR Identity
        EditText npub = view.findViewById(R.id.edit_npub);
        EditText nsec = view.findViewById(R.id.edit_nsec);
        npub.setText(settings.getNpub());
        nsec.setText(settings.getNsec());

        // Beacon Preferences
        Spinner beaconTypeSpinner = view.findViewById(R.id.spinner_beacon_type);
        EditText beaconNickname = view.findViewById(R.id.edit_beacon_nickname);
        EditText groupId = view.findViewById(R.id.edit_group_id);
        EditText deviceId = view.findViewById(R.id.edit_device_id);
        beaconNickname.setText(settings.getBeaconNickname());
        groupId.setText(settings.getIdGroup());
        deviceId.setText(settings.getIdDevice());

        String[] beaconTypes = getResources().getStringArray(R.array.beacon_types);
        for (int i = 0; i < beaconTypes.length; i++) {
            if (beaconTypes[i].equals(settings.getBeaconType())) {
                beaconTypeSpinner.setSelection(i);
                break;
            }
        }

        // Save Button
        View saveButton = view.findViewById(R.id.btn_save_settings);
        saveButton.setOnClickListener(v -> saveSettings(nickname, intro, npub, nsec, preferredColorSpinner, beaconTypeSpinner, groupId, deviceId));

        // Reset Button
        Button resetButton = view.findViewById(R.id.btn_reset_settings);
        resetButton.setOnClickListener(v -> {
            // Delete settings file
            SettingsLoader.deleteSettings(requireContext());

            // Reload settings and update the UI
            settings = SettingsLoader.loadSettings(requireContext());
            reloadSettings(view);

            //Toast.makeText(requireContext(), "Settings reset to defaults.", Toast.LENGTH_SHORT).show();
        });

        // Shutdown Button
        Button shutdownButton = view.findViewById(R.id.btn_shutdown_app);
        shutdownButton.setOnClickListener(v -> {
            requireActivity().finish();
            System.exit(0);
        });
    }

    private void reloadSettings(View view) {
        // Privacy Options
        Switch listenOnlySwitch = view.findViewById(R.id.switch_listen_only);
        listenOnlySwitch.setChecked(settings.isInvisibleMode());

        // User Preferences
        EditText nickname = view.findViewById(R.id.edit_preferred_nickname);
        EditText intro = view.findViewById(R.id.edit_intro);
        nickname.setText(settings.getNickname());
        intro.setText(settings.getIntro());

        Spinner preferredColorSpinner = view.findViewById(R.id.spinner_preferred_color);
        String[] colorOptions = getResources().getStringArray(R.array.color_options);
        for (int i = 0; i < colorOptions.length; i++) {
            if (colorOptions[i].equals(settings.getPreferredColor())) {
                preferredColorSpinner.setSelection(i);
                break;
            }
        }

        // NOSTR Identity
        EditText npub = view.findViewById(R.id.edit_npub);
        EditText nsec = view.findViewById(R.id.edit_nsec);
        npub.setText(settings.getNpub());
        nsec.setText(settings.getNsec());

        // Beacon Preferences
        Spinner beaconTypeSpinner = view.findViewById(R.id.spinner_beacon_type);
        EditText beaconNickname = view.findViewById(R.id.edit_beacon_nickname);
        EditText groupId = view.findViewById(R.id.edit_group_id);
        EditText deviceId = view.findViewById(R.id.edit_device_id);
        beaconNickname.setText(settings.getBeaconNickname());
        groupId.setText(settings.getIdGroup());
        deviceId.setText(settings.getIdDevice());

        String[] beaconTypes = getResources().getStringArray(R.array.beacon_types);
        for (int i = 0; i < beaconTypes.length; i++) {
            if (beaconTypes[i].equals(settings.getBeaconType())) {
                beaconTypeSpinner.setSelection(i);
                break;
            }
        }
    }

    private void saveSettings(SettingsUser settings){
        try{
            SettingsLoader.saveSettings(requireContext(), settings);
            Toast.makeText(requireContext(), "Settings saved successfully", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException e) {
            // Handle invalid values
            Toast.makeText(requireContext(), "Invalid input: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // Handle other errors
            Toast.makeText(requireContext(), "Error saving settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



    private void saveSettings(EditText nickname, EditText intro, EditText npub, EditText nsec,
                              Spinner preferredColorSpinner, Spinner beaconTypeSpinner, EditText groupId, EditText deviceId) {

        try {
            // Update settings object with validation
            settings.setNickname(nickname.getText().toString());
            settings.setIntro(intro.getText().toString());
            settings.setNpub(npub.getText().toString());
            settings.setNsec(nsec.getText().toString());
            settings.setPreferredColor(preferredColorSpinner.getSelectedItem().toString());
            settings.setBeaconType(beaconTypeSpinner.getSelectedItem().toString());
            settings.setBeaconNickname(((EditText) requireView().findViewById(R.id.edit_beacon_nickname)).getText().toString());
            settings.setIdGroup(groupId.getText().toString());
            settings.setIdDevice(deviceId.getText().toString());

            // Save settings using SettingsLoader
            SettingsLoader.saveSettings(requireContext(), settings);
            Toast.makeText(requireContext(), "Settings saved successfully", Toast.LENGTH_SHORT).show();

        } catch (IllegalArgumentException e) {
            // Handle invalid values
            Toast.makeText(requireContext(), "Invalid input: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // Handle other errors
            Toast.makeText(requireContext(), "Error saving settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
