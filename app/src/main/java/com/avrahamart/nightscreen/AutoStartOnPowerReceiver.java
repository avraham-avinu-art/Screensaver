package com.avrahamart.nightscreen;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;

public final class AutoStartOnPowerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_USER_PRESENT.equals(action)) {
            MainActivity.clearPausedByDeviceLockState();
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            if (!prefs.getBoolean("autoStartOnCharge", false)) return;
            if (!canLaunchFromBackground(context)) return;

            Intent launch = new Intent(context, MainActivity.class);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            launch.putExtra(MainActivity.EXTRA_OPENED_BY_CHARGER, true);
            try {
                context.startActivity(launch);
            } catch (Throwable ignored) {
                // Respect device-specific background-launch restrictions.
            }
            return;
        }

        if (Intent.ACTION_SCREEN_OFF.equals(action)
                && prefs.getBoolean("autoStartOnDeviceLock", false)
                && canLaunchFromBackground(context)) {
            // Let Android finish applying the keyguard state before checking it.
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> launchOnDeviceLockIfStillLocked(context.getApplicationContext()), 600L);
        }
    }

    private void launchOnDeviceLockIfStillLocked(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("autoStartOnDeviceLock", false)) return;
        if (!prefs.getBoolean("showOnLockScreen", Build.VERSION.SDK_INT >= 26)) return;
        if (MainActivity.isMainActivityResumed()
                || MainActivity.wasMainActivityPausedByDeviceLock()) return;

        PowerManager powerManager =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && powerManager.isInteractive()) return;

        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager == null || !keyguardManager.isKeyguardLocked()) return;
        if (!canLaunchFromBackground(context)) return;

        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(MainActivity.EXTRA_OPENED_BY_DEVICE_LOCK, true);
        try {
            context.startActivity(launch);
        } catch (Throwable ignored) {
            // Background activity launch restrictions vary by Android build.
        }
    }

    private boolean canLaunchFromBackground(Context context) {
        // Background activity starts on Android 10+ require the overlay permission
        // for this app's Android 11 target-device workflow.
        return Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(context);
    }
}
