package offgrid.geogram.devices.chat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import offgrid.geogram.R;
import offgrid.geogram.bluetooth.BlueQueueReceiving;
import offgrid.geogram.bluetooth.Bluecomm;
import offgrid.geogram.bluetooth.broadcast.BroadcastMessage;
import offgrid.geogram.bluetooth.eddystone.DeviceFinder;
import offgrid.geogram.core.Central;
import offgrid.geogram.core.Log;
import offgrid.geogram.devices.DeviceReachable;

public class DeviceChatFragment extends Fragment {

    private static final String ARG_DEVICE_ID = "device_id";
    private String deviceId;
    private final ArrayList<BroadcastMessage> displayedMessages = new ArrayList<>();

    private LinearLayout chatMessageContainer;
    private ScrollView chatScrollView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int REFRESH_INTERVAL_MS = 2000;
    Runnable runningPoll = null;

    public static DeviceChatFragment newInstance(String deviceId) {
        DeviceChatFragment fragment = new DeviceChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_ID, deviceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_broadcast_chat, container, false);

        if (getArguments() != null) {
            deviceId = getArguments().getString(ARG_DEVICE_ID);
        }

        if (deviceId == null) {
            Toast.makeText(getContext(), "Device Id is null", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        }

        Log.i("DeviceChatFragment", "Chatting with device: " + deviceId);

        // Initialize UI
        EditText messageInput = view.findViewById(R.id.message_input);
        ImageButton btnSend = view.findViewById(R.id.btn_send);
        chatMessageContainer = view.findViewById(R.id.chat_message_container);
        chatScrollView = view.findViewById(R.id.chat_scroll_view);

        // Back button
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // change the title
        TextView chatTitleTextView = view.findViewById(R.id.chat_title);
        chatTitleTextView.setText("Chat");

        // Send button
        btnSend.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (message.isEmpty()) {
                return;
            }

            //


            // Create a direct message
            BroadcastMessage messageToSend = new BroadcastMessage(message, Central.getInstance().getSettings().getIdDevice(), true);
            BlueQueueReceiving.getInstance(getContext()).addBroadcastMessage(messageToSend);

            new Thread(() -> {
                // get the updated MAC address (they change often)
                DeviceReachable deviceUpdated = DeviceFinder.getInstance(this.getContext()).getDeviceMap().get(deviceId);
                // when it is null, there is nothing to be done here
                if(deviceUpdated == null){
                    Toast.makeText(getContext(), "Device is not reachable", Toast.LENGTH_SHORT).show();
                    return;
                }
                //boolean success = BroadcastSender.sendToDevice(deviceId, messageToSend, getContext());
                Bluecomm.getInstance(getContext()).writeData(deviceUpdated.getMacAddress(),
                        messageToSend.getMessage());
                requireActivity().runOnUiThread(() -> {
                    //if (success) {
                        messageInput.setText("");
                        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
//                    } else {
//                        Toast.makeText(getContext(), "Failed to send message.", Toast.LENGTH_SHORT).show();
//                    }
                });
            }).start();
        });

        // Start polling for new messages
        startMessagePolling();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }

    private void startMessagePolling() {
        if (runningPoll != null) return;
        runningPoll = () -> {
            updateMessages();
            handler.postDelayed(runningPoll, REFRESH_INTERVAL_MS);
        };
        handler.postDelayed(runningPoll, REFRESH_INTERVAL_MS);
    }

    private void updateMessages() {
//        ArrayList<BroadcastMessage> messages = BlueQueueReceiving.getInstance(getContext()).getMessagesReceivedFromDevice(deviceId);
//        for (BroadcastMessage message : messages) {
//            if (!displayedMessages.contains(message)) {
//                addReceivedMessage(message);
//                displayedMessages.add(message);
//            }
//        }
//        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void addReceivedMessage(BroadcastMessage message) {
        View receivedMessageView = LayoutInflater.from(getContext()).inflate(R.layout.item_received_message, chatMessageContainer, false);
        TextView messageTextView = receivedMessageView.findViewById(R.id.message_user_1);
        messageTextView.setText(message.getMessage());
        chatMessageContainer.addView(receivedMessageView);
        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
    }
}
