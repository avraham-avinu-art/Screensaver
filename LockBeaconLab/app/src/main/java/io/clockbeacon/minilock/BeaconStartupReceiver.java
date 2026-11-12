package io.clockbeacon.minilock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

public final class BeaconStartupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;

        try {
            Bundle state = context.getContentResolver().call(
                    Uri.parse("content://" + BeaconControlProvider.AUTHORITY),
                    BeaconControlProvider.METHOD_FETCH, null, null);
            if (state == null
                    || !state.getBoolean(BeaconControlProvider.KEY_RUNNING, false)) return;

            Intent engine = new Intent(context, BeaconPulseService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(engine);
            } else {
                context.startService(engine);
            }
        } catch (RuntimeException ignored) {
            // A normal launcher start remains available if the provider is not ready.
        }
    }
}
