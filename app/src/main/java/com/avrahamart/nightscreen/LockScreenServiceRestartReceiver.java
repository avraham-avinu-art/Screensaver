package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Restores the shared automatic-start monitor after its scheduled recovery alarm. */
public final class LockScreenServiceRestartReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreensaverMonitor";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "restart alarm received; action="
                + (intent == null ? "null" : intent.getAction()));
        // Use the same feature and permission policy as normal service startup.
        // This includes boot-only and charging-only configurations.
        LockScreenMonitorService.sync(context);
    }
}
