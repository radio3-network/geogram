package offgrid.geogram.bluetooth.broadcast;

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
import offgrid.geogram.core.Log;
import offgrid.geogram.core.old.old.GenerateDeviceId;
import offgrid.geogram.util.DateUtils;

public class BroadcastChatFragment extends Fragment implements BroadcastSendMessage.MessageUpdateListener {

    private final ArrayList<BroadcastMessage> displayedMessages = new ArrayList<>();
    private LinearLayout chatMessageContainer;
    private ScrollView chatScrollView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int REFRESH_INTERVAL_MS = 10000;

    public BroadcastChatFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_broadcast_chat, container, false);

        // Initialize message input and send button
        EditText messageInput = view.findViewById(R.id.message_input);
        ImageButton btnSend = view.findViewById(R.id.btn_send);

        // Initialize chat message container and scroll view
        chatMessageContainer = view.findViewById(R.id.chat_message_container);
        chatScrollView = view.findViewById(R.id.chat_scroll_view);

        // Back button functionality
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Send button functionality
        btnSend.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if(message.isEmpty() || message.isBlank()){
                //Toast.makeText(getContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            // add this message to our list of sent messages
            String deviceId = GenerateDeviceId.generateInstanceId(this.getContext());
            BroadcastMessage messageToBroadcast = new BroadcastMessage(message, deviceId, true);
            BroadcastSendMessage.addMessage(messageToBroadcast);


            new Thread(() -> {
                // Send the message via BroadcastChat
                boolean success = BroadcastSendMessage.broadcast(messageToBroadcast, getContext());
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        messageInput.setText("");

                        // Scroll to the bottom of the chat
                        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
                    } else {
                        Toast.makeText(getContext(), "Failed to send message. Please check Bluetooth.", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();

        });

        // Start message polling
        startMessagePolling();

        // Register the fragment as a listener for updates
        BroadcastSendMessage.setMessageUpdateListener(this);

        // update the message right now on the chat box
        updateMessages();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop message polling and unregister listener to avoid memory leaks
        handler.removeCallbacksAndMessages(null);
        BroadcastSendMessage.removeMessageUpdateListener();
    }

    /**
     * Starts polling the BroadcastChat.messages list every 10 seconds.
     */
    private void startMessagePolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateMessages();
                handler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        }, REFRESH_INTERVAL_MS);
    }

    /**
     * Updates the chat message container with new messages.
     */
    private void updateMessages() {
        ArrayList<BroadcastMessage> currentMessages = new ArrayList<>(BroadcastSendMessage.messages);
        for (BroadcastMessage message : currentMessages) {
            if (!displayedMessages.contains(message)) {
                if (message.isWrittenByMe()) {
                    Log.i("BroadcastChatFragment", "Adding user message: " + message.getMessage());
                    addUserMessage(message);
                } else {
                    Log.i("BroadcastChatFragment", "Adding received message: " + message.getMessage());
                    addReceivedMessage(message);
                    // Scroll to the bottom of the chat
                    chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
                }
                displayedMessages.add(message);
            }
        }
    }

    /**
     * Adds a user message to the chat message container.
     *
     * @param message The message to display.
     */
    private void addUserMessage(BroadcastMessage message) {
        View userMessageView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_user_message, chatMessageContainer, false);
        TextView messageTextView = userMessageView.findViewById(R.id.message_user_self);
        messageTextView.setText(message.getMessage());

        // add the other details
        TextView textBoxUpper = userMessageView.findViewById(R.id.upper_text);
        TextView textBoxLower = userMessageView.findViewById(R.id.lower_text);

        long timeStamp = message.getTimestamp();
        String dateText = DateUtils.convertTimestampForChatMessage(timeStamp);
        textBoxUpper.setText(dateText);
        textBoxLower.setText("");



        chatMessageContainer.addView(userMessageView);
        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
    }

    /**
     * Adds a received message to the chat message container.
     *
     * @param message The message to display.
     */
    public void addReceivedMessage(BroadcastMessage message) {
        View receivedMessageView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_received_message, chatMessageContainer, false);

        // Get the objects
        TextView textBoxUpper = receivedMessageView.findViewById(R.id.sender_name);
        TextView textBoxLower = receivedMessageView.findViewById(R.id.message_timestamp);

        // Set the sender's name
        if(message.getDeviceId() != null){
            textBoxLower.setText(message.getDeviceId());
        }else{
            textBoxLower.setText("");
        }
        // now add the time stamp
        long timeStamp = message.getTimestamp();
        String dateText = DateUtils.convertTimestampForChatMessage(timeStamp);
        textBoxUpper.setText(dateText);


        // Set the message content
        TextView messageTextView = receivedMessageView.findViewById(R.id.message_user_1);
        messageTextView.setText(message.getMessage());

        // Add the view to the container
        chatMessageContainer.addView(receivedMessageView);
        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    public void onMessageUpdate() {
        // Update the messages immediately when notified
        updateMessages();
    }
}
