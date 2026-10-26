package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

/** Restarts opt-in lock monitoring if a device terminates its foreground service on task removal. */
public final class LockScreenServiceRestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("autoStartOnDeviceLock", false)) return;
        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) return;

        Intent service = new Intent(context, LockScreenMonitorService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (Throwable ignored) {
            // Android versions and device vendors can restrict background restarts.
        }
    }
}
