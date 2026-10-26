package com.avrahamart.nightscreen;

import android.app.ActivityManager;
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
                && LockScreenMonitorService.isLockMonitoringActive()
                && canLaunchFromBackground(context)) {
            // Let Android finish applying the keyguard state before checking it.
            PowerManager powerManager =
                    (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = null;
            try {
                if (powerManager != null) {
                    wakeLock = powerManager.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK,
                            "NightScreen:LockStart");
                    wakeLock.acquire(3_000L);
                }
            } catch (Throwable ignored) {
                wakeLock = null;
            }
            final PowerManager.WakeLock heldWakeLock = wakeLock;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    launchOnDeviceLockIfStillLocked(context.getApplicationContext());
                } finally {
                    if (heldWakeLock != null) {
                        try {
                            if (heldWakeLock.isHeld()) heldWakeLock.release();
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }, 450L);
        }
    }

    private void launchOnDeviceLockIfStillLocked(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!LockScreenMonitorService.isLockMonitoringActive()) return;
        if (!prefs.getBoolean("showOnLockScreen", Build.VERSION.SDK_INT >= 26)) return;
        if (isMainActivityVisible(context)) return;

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

    private boolean isMainActivityVisible(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return false;
        try {
            java.util.List<ActivityManager.RunningAppProcessInfo> processes =
                    activityManager.getRunningAppProcesses();
            if (processes == null) return false;
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (context.getPackageName().equals(process.processName)) {
                    // Treat a visible, resumed, or top-sleeping Activity as already present.
                    return process.importance
                            <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private boolean canLaunchFromBackground(Context context) {
        // Background activity starts on Android 10+ require the overlay permission
        // for this app's Android 11 target-device workflow.
        return Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(context);
    }
}
