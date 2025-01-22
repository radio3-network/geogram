package offgrid.geogram.wifi.messages;

import offgrid.geogram.protocol.data.BioProfile_v1;
import offgrid.geogram.wifi.comm.CID;

public class MessageHello_v1 extends Message{

    final BioProfile_v1 bioProfile;

    // container values
    private String version = "1";

    public MessageHello_v1() {
        super(CID.HELLO);
        this.bioProfile = BioProfile_v1.getLocalUser();
    }
}
