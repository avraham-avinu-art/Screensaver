package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public final class AutoStartOnPowerReceiver extends BroadcastReceiver {
    private static final String TAG = "NightScreenAutoStart";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
            saveStatus(prefs, "power_disconnected");
            Log.i(TAG, "Power disconnected; waiting for the next connection.");
            return;
        }
        if (!Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) return;

        if (!prefs.getBoolean("autoStartOnCharge", false)) {
            saveStatus(prefs, "switch_off");
            Log.i(TAG, "Power connected, but automatic start is disabled.");
            return;
        }

        // Android 10+ restricts background activity launches. SYSTEM_ALERT_WINDOW
        // is a documented exception, and is requested explicitly in SettingsActivity.
        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) {
            saveStatus(prefs, "permission_missing");
            Log.w(TAG, "Power connected, but overlay permission is missing.");
            return;
        }

        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(MainActivity.EXTRA_OPENED_BY_CHARGER, true);

        // Store the attempt before launching. MainActivity changes this to "opened"
        // once it receives the charger intent. If it stays at "launch_requested",
        // Android or the device manufacturer may have blocked the background launch.
        saveStatus(prefs, "launch_requested");
        try {
            context.startActivity(launch);
            Log.i(TAG, "Power connected; requested MainActivity launch.");
        } catch (Throwable error) {
            saveStatus(prefs, "launch_error:" + error.getClass().getSimpleName());
            Log.e(TAG, "Unable to launch MainActivity after power connected.", error);
        }
    }

    private static void saveStatus(SharedPreferences prefs, String status) {
        prefs.edit()
                .putString("autoStartLastStatus", status)
                .putLong("autoStartLastTime", System.currentTimeMillis())
                .apply();
    }
}
