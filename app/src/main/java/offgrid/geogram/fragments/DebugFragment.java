package offgrid.geogram.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import offgrid.geogram.R;
import offgrid.geogram.core.Log;
import offgrid.geogram.settings.SettingsFragment;
import offgrid.geogram.settings.SettingsLoader;

public class DebugFragment extends Fragment {

    private static DebugFragment instance;

    private LinearLayout logTab;
    private LinearLayout optionsTab;
    private Button tabLogButton;
    private Button tabOptionsButton;
    private EditText logFilter;
    private boolean isPaused = false; // Log pause state
    private List<String> allLogMessages = new ArrayList<>(); // Store all log messages
    private TextView logWindow;
    private ScrollView logScrollView;

    // Private constructor to prevent direct instantiation
    private DebugFragment() {
        // Required empty public constructor
    }

    // Singleton instance getter
    public static DebugFragment getInstance() {
        if (instance == null) {
            instance = new DebugFragment();
        }
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_debug, container, false);

        // Tabs and their content
        logTab = view.findViewById(R.id.log_tab);
        optionsTab = view.findViewById(R.id.options_tab);
        tabLogButton = view.findViewById(R.id.tab_log);
        tabOptionsButton = view.findViewById(R.id.tab_options);
        logFilter = view.findViewById(R.id.log_filter);

        // Initial state: Log tab is active
        switchTab(true);

        // Tab button listeners
        tabLogButton.setOnClickListener(v -> switchTab(true));
        tabOptionsButton.setOnClickListener(v -> switchTab(false));

        // Initialize log window
        logWindow = view.findViewById(R.id.lv_log);
        logScrollView = view.findViewById(R.id.log_scroll_view);

        logUpdateAllMessages();

        // Back button functionality
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Clear log button functionality
        ImageButton btnClearLog = view.findViewById(R.id.btn_clear_log);
        btnClearLog.setOnClickListener(v -> {
            allLogMessages.clear();
            logWindow.setText("");
            Toast.makeText(getContext(), "Log cleared", Toast.LENGTH_SHORT).show();
        });

        // Copy to clipboard button functionality
        ImageButton btnCopyLog = view.findViewById(R.id.btn_copy_log);
        btnCopyLog.setOnClickListener(v -> {
            String logContent = logWindow.getText().toString();
            if (!logContent.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Filtered Log Messages", logContent);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Filtered log copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Log is empty", Toast.LENGTH_SHORT).show();
            }
        });

        // Pause/Play log button functionality
        ImageButton btnPauseLog = view.findViewById(R.id.btn_pause_log);
        btnPauseLog.setOnClickListener(v -> {
            isPaused = !isPaused;
            btnPauseLog.setImageResource(isPaused ? R.drawable.ic_autorenew : R.drawable.ic_pause);
            Toast.makeText(getContext(), isPaused ? "Log paused" : "Log resumed", Toast.LENGTH_SHORT).show();
        });


        // buttons for the options

        // Shutdown Button
        Button shutdownButton = view.findViewById(R.id.btn_shutdown_app);
        shutdownButton.setOnClickListener(v -> {
            requireActivity().finish();
            System.exit(0);
        });

        // Reset Button
        Button resetButton = view.findViewById(R.id.btn_reset_settings);
        resetButton.setOnClickListener(v -> {
            // Delete settings file
            SettingsLoader.deleteSettings(requireContext());

            // Reload settings and update the UI
            //SettingsFragment.getInstance().reloadSettings();
            Toast.makeText(requireContext(), "Settings reset to defaults.", Toast.LENGTH_SHORT).show();
        });



        // Filter log messages
        EditText logFilter = view.findViewById(R.id.log_filter);
        logFilter.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                logUpdateAllMessages();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // No-op
            }
        });

        return view;
    }

    /**
     * Switch between tabs by updating their styles and visibility.
     *
     * @param showLogTab If true, display the Log tab; otherwise, display the Options tab.
     */
    private void switchTab(boolean showLogTab) {
        if (showLogTab) {
            logTab.setVisibility(View.VISIBLE);
            optionsTab.setVisibility(View.GONE);
            tabLogButton.setBackgroundColor(getResources().getColor(R.color.neon_green));
            tabOptionsButton.setBackgroundColor(getResources().getColor(R.color.dark_gray));
            tabLogButton.setTextColor(getResources().getColor(R.color.black));
            tabOptionsButton.setTextColor(getResources().getColor(R.color.white));
        } else {
            logTab.setVisibility(View.GONE);
            optionsTab.setVisibility(View.VISIBLE);
            tabOptionsButton.setBackgroundColor(getResources().getColor(R.color.neon_green));
            tabLogButton.setBackgroundColor(getResources().getColor(R.color.dark_gray));
            tabOptionsButton.setTextColor(getResources().getColor(R.color.black));
            tabLogButton.setTextColor(getResources().getColor(R.color.white));
        }
    }




    public void logUpdateAllMessages(){
        // don't write anything during pauses
        if(isPaused){
            return;
        }
        // update the text
        if(logWindow == null){
            return;
        }

        logWindow.post(() -> {
            String text = "";

            String filter = logFilter.getText().toString().toLowerCase();

            for(String message : Log.logMessages){
                if(isPaused){
                    continue;
                }
                // no filter, show everything
                if(filter.isEmpty()){
                    text = text.concat(message).concat("\n");
                    continue;
                }
                // only add the message when the filter is applicable
                if(message.toLowerCase().contains(filter)){
                    text = text.concat(message).concat("\n");
                }
            }

            // write everything on the UI
            logWindow.setText(text);
            logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    /**
     * Handles new log messages, ensuring paused state is respected.
     *
     * @param message The new log message to add.
     */
    private void onNewLogMessage(String message) {
        allLogMessages.add(message);
        if (!isPaused) {
            logWindow.append(message + "\n");
        }
    }
}
