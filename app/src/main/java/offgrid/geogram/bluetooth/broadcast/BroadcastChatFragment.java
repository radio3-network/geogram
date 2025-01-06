package offgrid.geogram.bluetooth.broadcast;

import static offgrid.geogram.bluetooth.comms.BlueCommands.tagBio;
import static offgrid.geogram.bluetooth.comms.BlueQueue.messagesReceivedAsBroadcast;

import android.graphics.drawable.GradientDrawable;
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
import java.util.HashMap;

import offgrid.geogram.R;
import offgrid.geogram.core.Central;
import offgrid.geogram.core.Log;
import offgrid.geogram.database.BioDatabase;
import offgrid.geogram.database.BioProfile;
import offgrid.geogram.util.ASCII;
import offgrid.geogram.util.DateUtils;

public class BroadcastChatFragment extends Fragment implements BroadcastSendMessage.MessageUpdateListener {

    // messages that are displayed
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
            String deviceId = Central.getInstance().getSettings().getIdDevice();
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
        ArrayList<BroadcastMessage> currentMessages = new ArrayList<>(messagesReceivedAsBroadcast);
        for (BroadcastMessage message : currentMessages) {
            if (!displayedMessages.contains(message)) {
                if (message.isWrittenByMe()) {
                    Log.i("BroadcastChatFragment", "Adding user message: " + message.getMessage());
                    addUserMessage(message);
                } else {
                    // save the bio profile to disk
                    String messageText = message.getMessage();
                    if(messageText.startsWith(tagBio)){
                        String data = messageText.substring(tagBio.length());
                        BioProfile profile = BioProfile.fromJson(data);
                        if(profile == null){
                            Log.e("BroadcastChatFragment", "Invalid bio profile received: " + data);
                            return;
                        }
                        // valid bio, write it to our database
                        BioDatabase.save(message.getDeviceId(), profile, this.getContext());
                        Log.i("BroadcastChatFragment", "Adding bio profile: " + profile.getNick());
                        //return;
                    }

                    Log.i("BroadcastChatFragment", "Adding received message: "
                            + message.getMessage() + " from " + message.getDeviceId());
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

        BioProfile profile = BioDatabase.get(message.getDeviceId(), this.getContext());
        String nickname = "";

        if (profile != null) {
            nickname = profile.getNick();
        }

        // Set the sender's name
        if (nickname.isEmpty() && message.getDeviceId() != null) {
            textBoxLower.setText(message.getDeviceId());
        } else {
            textBoxLower.setText(nickname);
        }

        // Add the timestamp
        long timeStamp = message.getTimestamp();
        String dateText = DateUtils.convertTimestampForChatMessage(timeStamp);
        textBoxUpper.setText(dateText);

        // Set the message content
        String text = message.getMessage();
        if(text.startsWith(tagBio) && profile != null){
            if(profile.getExtra() == null){
                // add a nice one line ASCII emoticon
                profile.setExtra(ASCII.getRandomOneliner());
                BioDatabase.save(profile.getDeviceId(), profile, this.getContext());
            }
            text = profile.getExtra();
        }
        TextView messageTextView = receivedMessageView.findViewById(R.id.message_user_1);
        messageTextView.setText(text);

        // Apply balloon style based on preferred background color
        String colorBackground = profile != null ? profile.getColor() : "light gray";
        applyBalloonStyle(messageTextView, colorBackground);

        // Add the view to the container
        chatMessageContainer.addView(receivedMessageView);
        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
    }


    private void applyBalloonStyle(TextView messageTextView, String backgroundColor) {
        int bgColor;
        int textColor;

        // Define readable text color based on the background color
        switch (backgroundColor.toLowerCase()) {
            case "black":
                bgColor = getResources().getColor(R.color.black);
                textColor = getResources().getColor(R.color.white);
                break;
            case "yellow":
                bgColor = getResources().getColor(R.color.yellow);
                textColor = getResources().getColor(R.color.black);
                break;
            case "blue":
            case "green":
            case "cyan":
            case "red":
            case "magenta":
            case "pink":
            case "brown":
            case "dark gray":
            case "light red":
            case "white":
                bgColor = getResources().getColor(getResources().getIdentifier(backgroundColor.replace(" ", "_").toLowerCase(), "color", requireContext().getPackageName()));
                textColor = backgroundColor.equalsIgnoreCase("white") ? getResources().getColor(R.color.black) : getResources().getColor(R.color.white);
                break;
            case "light blue":
            case "light green":
            case "light cyan":
                bgColor = getResources().getColor(getResources().getIdentifier(backgroundColor.replace(" ", "_").toLowerCase(), "color", requireContext().getPackageName()));
                textColor = getResources().getColor(R.color.black);
                break;

            default:
                // Fallback to a neutral background and readable text color
                bgColor = getResources().getColor(R.color.light_gray); // Define a light gray fallback in colors.xml
                textColor = getResources().getColor(R.color.black);
                break;
        }

        // Apply the background and text colors to the message TextView
        if (messageTextView.getBackground() instanceof GradientDrawable) {
            GradientDrawable background = (GradientDrawable) messageTextView.getBackground();
            background.setColor(bgColor);
        }
        messageTextView.setTextColor(textColor);
    }


    @Override
    public void onMessageUpdate() {
        // Update the messages immediately when notified
        updateMessages();
    }
}
