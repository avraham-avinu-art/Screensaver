package io.clockbeacon.minilock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

public final class BeaconRestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Boolean.TRUE.equals(readEnabledState(context))) return;
        if (!Settings.canDrawOverlays(context)) return;
        if (!BeaconAccessibilityService.isServiceEnabled(context)) return;

        Intent engine = new Intent(context, BeaconPulseService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(engine);
            } else {
                context.startService(engine);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private Boolean readEnabledState(Context context) {
        try {
            Bundle state = context.getContentResolver().call(
                    Uri.parse("content://" + BeaconControlProvider.AUTHORITY),
                    BeaconControlProvider.METHOD_FETCH, null, null);
            return state == null ? null
                    : state.getBoolean(BeaconControlProvider.KEY_RUNNING, false);
        } catch (RuntimeException error) {
            return null;
        }
    }
}
