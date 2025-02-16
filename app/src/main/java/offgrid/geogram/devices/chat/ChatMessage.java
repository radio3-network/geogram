package offgrid.geogram.devices.chat;

import java.util.ArrayList;

public class ChatMessage {

    public String senderDeviceId;
    public String message;
    public long timestamp;
    // SHA1 list of attachments
    public ArrayList<String> attachments = new ArrayList<>();


}
