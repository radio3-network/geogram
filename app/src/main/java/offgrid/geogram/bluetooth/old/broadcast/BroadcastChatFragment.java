package offgrid.geogram.bluetooth.old.broadcast;

import android.os.Bundle;
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

import offgrid.geogram.R;

public class BroadcastChatFragment extends Fragment {

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
        LinearLayout chatMessageContainer = view.findViewById(R.id.chat_message_container);
        ScrollView chatScrollView = view.findViewById(R.id.chat_scroll_view);

        // Back button functionality
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Send button functionality
        btnSend.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                addUserMessage(chatMessageContainer, message);
                messageInput.setText("");

                // Scroll to the bottom of the chat
                chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
            } else {
                Toast.makeText(getContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    /**
     * Adds a user message to the chat message container.
     *
     * @param chatMessageContainer The LinearLayout containing chat messages.
     * @param message              The message to display.
     */
    private void addUserMessage(LinearLayout chatMessageContainer, String message) {

        // first let's broadcast the message
        BroadcastChat.broadcast(message);

        // Inflate the custom layout for user-sent messages
        View userMessageView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_user_message, chatMessageContainer, false);

        // Find the TextView and set the message text
        TextView messageTextView = userMessageView.findViewById(R.id.message_user_self);
        messageTextView.setText(message);

        // Add the user message to the chat container
        chatMessageContainer.addView(userMessageView);
    }


    /**
     * Adds a received message to the chat message container.
     * This can be called when a message is received from other users.
     *
     * @param chatMessageContainer The LinearLayout containing chat messages.
     * @param message              The message to display.
     */
    public void addReceivedMessage(LinearLayout chatMessageContainer, String message) {
        // Inflate the custom layout for received messages
        View receivedMessageView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_received_message, chatMessageContainer, false);

        // Find the TextView and set the message text
        TextView messageTextView = receivedMessageView.findViewById(R.id.message_user_1);
        messageTextView.setText(message);

        // Apply the speech bubble background for received messages
        messageTextView.setBackgroundResource(R.drawable.balloon_left);

        // Add the message view to the chat container
        chatMessageContainer.addView(receivedMessageView);
    }
}
