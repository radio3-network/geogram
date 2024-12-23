package offgrid.geogram.settings;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import java.io.File;

public class SettingsUser {

    @Expose
    String nickname;

    @Expose
    String intro;

    @Expose
    boolean invisibleMode;

    @Expose
    String npub;

    @Expose
    String nsec;

    @Expose
    String beaconType;

    @Expose
    String idGroup;

    @Expose
    String idDevice;

}
