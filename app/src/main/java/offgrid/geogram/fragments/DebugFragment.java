package offgrid.geogram.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import offgrid.geogram.R;
import offgrid.geogram.core.Log;

public class DebugFragment extends Fragment {

    public DebugFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_debug, container, false);

        // Initialize log window
        EditText logWindow = view.findViewById(R.id.lv_log);
        Log.setLogWindow(logWindow);
        Log.logUpdate();

        // Back button functionality
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Clear log button functionality
        ImageButton btnClearLog = view.findViewById(R.id.btn_clear_log);
        btnClearLog.setOnClickListener(v -> {
            Log.clear(); 
            Toast.makeText(getContext(), "Log cleared", Toast.LENGTH_SHORT).show();
        });

        // Copy to clipboard button functionality
        ImageButton btnCopyLog = view.findViewById(R.id.btn_copy_log);
        btnCopyLog.setOnClickListener(v -> {
            String logContent = logWindow.getText().toString();
            if (!logContent.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Log Messages", logContent);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Log copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Log is empty", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
