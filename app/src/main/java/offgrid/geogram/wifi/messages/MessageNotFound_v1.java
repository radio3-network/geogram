package offgrid.geogram.wifi.messages;

import offgrid.geogram.protocol.data.BioProfile_v1;
import offgrid.geogram.wifi.comm.CID;

public class MessageNotFound_v1 extends Message{
    public MessageNotFound_v1() {
        super(CID.NOT_FOUND);
    }
}
