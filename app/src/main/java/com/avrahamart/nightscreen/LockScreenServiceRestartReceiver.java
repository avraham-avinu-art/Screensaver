package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restores the shared automatic-start monitor after its scheduled recovery alarm. */
public final class LockScreenServiceRestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Use the same feature and permission policy as normal service startup.
        // This includes boot-only and charging-only configurations.
        LockScreenMonitorService.sync(context);
    }
}
