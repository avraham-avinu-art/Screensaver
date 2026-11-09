package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/** Handles only system boot and app replacement; never treats either as a lock event. */
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
            // Each completed reboot begins a fresh lock cycle.
            prefs.edit().putBoolean("deviceLockStartConsumed", false).apply();
        }

        // Starts the monitor only if lock, wake, or app-managed charging is enabled.
        // Boot itself is exclusively handled below.
        LockScreenMonitorService.sync(context);

        boolean bootEnabled = AutoStartPolicy.isEnabledForAction(
                action,
                false,
                prefs.getBoolean("autoStartOnBoot", false),
                false,
                false,
                false);
        if (!bootEnabled) return;

        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Boot auto-start is enabled, but SYSTEM_ALERT_WINDOW was not granted.");
            return;
        }

        // The PendingIntent includes the explicit BAL sender/creator opt-ins used
        // by Android 14+ and Android 15+ for a launch requested from a receiver.
        boolean launched = AutoStartActivityLauncher.launch(
                context, AutoStartPolicy.Trigger.DEVICE_BOOT);
        if (!launched) {
            Log.e(TAG, "Unable to start the screensaver from BOOT_COMPLETED.");
        }
    }
}
