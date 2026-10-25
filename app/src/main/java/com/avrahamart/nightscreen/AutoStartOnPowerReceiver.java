package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

public final class AutoStartOnPowerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null
                || !Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("autoStartOnCharge", false)) {
            return;
        }

        // Android 10+ restricts background activity launches. SYSTEM_ALERT_WINDOW
        // is a documented exception, and is requested explicitly in SettingsActivity.
        // Without this permission, do not pretend that a notification is automatic
        // launch: leave the screensaver closed and explain the missing permission in settings.
        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) {
            return;
        }

        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(MainActivity.EXTRA_OPENED_BY_CHARGER, true);

        try {
            context.startActivity(launch);
        } catch (Throwable ignored) {
            // Respect device-specific background-launch restrictions; do not
            // substitute a manual notification for the requested auto-start behavior.
        }
    }
}
