package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

/** Restores opt-in lock monitoring after a normal device reboot or app replacement. */
public final class LockScreenBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean autoStartOnBoot = prefs.getBoolean("autoStartOnBoot", false);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // A reboot starts a new lock lifecycle; discard a latch from the
            // previous boot so the next real lock event can be handled.
            prefs.edit().putBoolean("deviceLockStartConsumed", false).apply();
        }
        LockScreenMonitorService.sync(context);

        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) || !autoStartOnBoot) return;
        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) return;

        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(MainActivity.EXTRA_OPENED_BY_BOOT, true);
        try {
            context.startActivity(launch);
        } catch (Throwable ignored) {
            // Android background-launch restrictions vary by device and version.
        }
    }
}
