package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/** Handles only device boot and app replacement. It never handles lock or wake. */
public final class LockScreenBootReceiver extends BroadcastReceiver {
    private static final String TAG = "NightScreenAutoStart";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;

        SharedPreferences prefs =
                context.getSharedPreferences("settings", Context.MODE_PRIVATE);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // A reboot starts a new lock cycle.
            prefs.edit().putBoolean("deviceLockStartConsumed", false).apply();
        }

        // This call starts monitoring only for lock, wake, or app-managed charge.
        // Boot itself is handled by this manifest receiver and needs no service.
        LockScreenMonitorService.sync(context);

        boolean bootTrigger = AutoStartPolicy.isEnabledForAction(
                action,
                false,
                prefs.getBoolean("autoStartOnBoot", false),
                false,
                false,
                false);
        if (!bootTrigger) return;

        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Boot auto-start is enabled, but the overlay permission is missing.");
            return;
        }

        // Use the same source-specific PendingIntent launcher as the other triggers.
        // On Android 14/15+, it explicitly opts in to sender/creator BAL privileges.
        AutoStartActivityLauncher.launch(context, AutoStartPolicy.Trigger.DEVICE_BOOT);
    }
}
