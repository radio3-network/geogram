package offgrid.geogram.old;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootServiceTIO extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, ServiceTIO.class);
            context.startService(serviceIntent);
        }
    }

}
