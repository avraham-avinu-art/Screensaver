package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** Disables the foreground lock/wake monitor when its notification is tapped. */
public final class CancelAutoStartServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs =
                context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean("autoStartOnDeviceLock", false)
                .putBoolean("autoStartOnWake", false)
                .putBoolean("deviceLockActivationConsumed", false)
                .apply();
        LockScreenMonitorService.sync(context);
    }
}
