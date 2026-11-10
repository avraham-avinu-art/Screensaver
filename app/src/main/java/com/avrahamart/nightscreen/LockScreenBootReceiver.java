package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public final class LockScreenBootReceiver extends BroadcastReceiver {
    private static final String TAG = "NightScreenAutoStart";
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            prefs.edit().putBoolean("deviceLockStartConsumed", false).apply();
        }
        LockScreenMonitorService.sync(context);
        boolean enabled = AutoStartPolicy.isEnabledForAction(
                action, false, prefs.getBoolean("autoStartOnBoot", false), false, false, false);
        if (!enabled) return;
        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Boot auto-start requires SYSTEM_ALERT_WINDOW.");
            return;
        }
        if (!AutoStartActivityLauncher.launch(context, AutoStartPolicy.Trigger.DEVICE_BOOT)) {
            Log.e(TAG, "Unable to start the screensaver from BOOT_COMPLETED.");
        }
    }
}
