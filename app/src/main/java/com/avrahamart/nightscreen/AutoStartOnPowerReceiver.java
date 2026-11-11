package com.avrahamart.nightscreen;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public final class AutoStartOnPowerReceiver extends BroadcastReceiver {
    private static final long DEEP_SLEEP_MIN_MS = 1_000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        MonitorDiagnostics.record(context, "AutoStartOnPowerReceiver received action=" + action
                + ", pid=" + android.os.Process.myPid());

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (Intent.ACTION_USER_PRESENT.equals(action)) {
            MonitorDiagnostics.record(context, "USER_PRESENT received; resetting lock-cycle state.");
            MonitorSettingsSnapshot.resetDeviceLockActivation(context);
            prefs.edit().remove("deepSleepScreenOffElapsed")
                    .remove("deepSleepScreenOffUptime").apply();
            MainActivity.clearPausedByDeviceLockState();
            return;
        }

        if (Intent.ACTION_SCREEN_ON.equals(action)) {
            handleScreenOn(context, prefs);
            return;
        }
        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            long screenOffElapsed = SystemClock.elapsedRealtime();
            long screenOffUptime = SystemClock.uptimeMillis();
            boolean saved = prefs.edit()
                    .putLong("deepSleepScreenOffElapsed", screenOffElapsed)
                    .putLong("deepSleepScreenOffUptime", screenOffUptime)
                    .commit();
            MonitorDiagnostics.record(context, "SCREEN_OFF timestamp saved=" + saved
                    + ", elapsed=" + screenOffElapsed + ", uptime=" + screenOffUptime);
        }

        MonitorSettingsSnapshot settings = MonitorSettingsSnapshot.read(context);
        if (settings == null) {
            MonitorDiagnostics.record(context, "Event ignored: monitor settings snapshot is unavailable.");
            return;
        }

        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            MonitorDiagnostics.record(context, "POWER_CONNECTED state: enabled="
                    + settings.autoStartOnCharge + ", androidDream="
                    + settings.useAndroidDreamForCharging + ", overlayGranted="
                    + canLaunchFromBackground(context));
            if (!settings.autoStartOnCharge || settings.useAndroidDreamForCharging) return;
            if (!canLaunchFromBackground(context)) {
                MonitorDiagnostics.record(context, "POWER_CONNECTED ignored: overlay permission unavailable.");
                return;
            }
            if (!claimPowerConnectedEvent(context)) {
                MonitorDiagnostics.record(context, "POWER_CONNECTED ignored: duplicate event guard claimed it.");
                return;
            }

            Intent launch = new Intent(context, MainActivity.class);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            launch.putExtra(MainActivity.EXTRA_OPENED_BY_CHARGER, true);
            try {
                context.startActivity(launch);
            } catch (RuntimeException error) {
                MonitorDiagnostics.record(context, "POWER_CONNECTED startActivity failed; exception="
                        + error.getClass().getName() + ": " + error.getMessage());
            }
            return;
        }

        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            MonitorDiagnostics.record(context, "SCREEN_OFF lock-start gate: enabled="
                    + settings.autoStartOnDeviceLock
                    + ", consumed=" + settings.deviceLockActivationConsumed
                    + ", monitorActive=" + LockScreenMonitorService.isLockMonitoringActive()
                    + ", overlayGranted=" + canLaunchFromBackground(context));
        }
        if (Intent.ACTION_SCREEN_OFF.equals(action)
                && settings.autoStartOnDeviceLock
                && !settings.deviceLockActivationConsumed
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
        MonitorSettingsSnapshot settings = MonitorSettingsSnapshot.read(context);
        if (!LockScreenMonitorService.isLockMonitoringActive()
                || settings == null || !settings.autoStartOnDeviceLock
                || settings.deviceLockActivationConsumed) return;

        PowerManager powerManager =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && powerManager.isInteractive()) return;

        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager == null || !keyguardManager.isKeyguardLocked()) return;
        if (!canLaunchFromBackground(context)) return;

        if (!MonitorSettingsSnapshot.claimDeviceLockActivation(context)) return;
        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(MainActivity.EXTRA_OPENED_BY_DEVICE_LOCK, true);
        try {
            context.startActivity(launch);
        } catch (Throwable ignored) {
            MonitorSettingsSnapshot.resetDeviceLockActivation(context);
            // Background activity launch restrictions vary by Android build.
        }
    }

    private void handleScreenOn(Context context, SharedPreferences prefs) {
        long offElapsed = prefs.getLong("deepSleepScreenOffElapsed", -1L);
        long offUptime = prefs.getLong("deepSleepScreenOffUptime", -1L);
        long nowElapsed = SystemClock.elapsedRealtime();
        long nowUptime = SystemClock.uptimeMillis();
        boolean deepSleep = offElapsed >= 0L && offUptime >= 0L
                && nowElapsed >= offElapsed && nowUptime >= offUptime
                && (nowElapsed - offElapsed) - (nowUptime - offUptime) >= DEEP_SLEEP_MIN_MS;
        prefs.edit().remove("deepSleepScreenOffElapsed")
                .remove("deepSleepScreenOffUptime").apply();

        MonitorSettingsSnapshot settings = MonitorSettingsSnapshot.read(context);
        boolean afterDeepSleep = settings != null && settings.autoStartOnWake
                && deepSleep && !settings.deviceLockActivationConsumed;
        boolean monitorActive = LockScreenMonitorService.isLockMonitoringActive();
        boolean activityVisible = isMainActivityVisible(context);
        boolean overlayGranted = canLaunchFromBackground(context);
        MonitorDiagnostics.record(context, "SCREEN_ON wake gate: deepSleep=" + deepSleep
                + ", autoStartOnWake=" + (settings != null && settings.autoStartOnWake)
                + ", lockCycleConsumed=" + (settings != null && settings.deviceLockActivationConsumed)
                + ", monitorActive=" + monitorActive + ", mainActivityVisible="
                + activityVisible + ", overlayGranted=" + overlayGranted);
        if (!afterDeepSleep || !monitorActive || activityVisible || !overlayGranted) return;

        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(MainActivity.EXTRA_OPENED_BY_WAKE, true);
        try {
            context.startActivity(launch);
        } catch (RuntimeException error) {
            MonitorDiagnostics.record(context, "SCREEN_ON startActivity failed; exception="
                    + error.getClass().getName() + ": " + error.getMessage());
        }
    }

    private boolean claimPowerConnectedEvent(Context context) {
        // A short cross-process file lock prevents two already-running monitor
        // processes from launching the screensaver twice for one plug-in event.
        synchronized (AutoStartOnPowerReceiver.class) {
            File guardFile = new File(
                    context.getNoBackupFilesDir(), "power_connected_guard.bin");
            long now = SystemClock.elapsedRealtime();
            try (RandomAccessFile file = new RandomAccessFile(guardFile, "rw");
                 FileChannel channel = file.getChannel();
                 FileLock ignored = channel.lock()) {
                long previous = -10_000L;
                if (file.length() >= 8L) {
                    file.seek(0L);
                    previous = file.readLong();
                }
                if (now >= previous && now - previous < 3_000L) return false;
                file.setLength(0L);
                file.seek(0L);
                file.writeLong(now);
                return true;
            } catch (Throwable ignored) {
                // Preserve the charger action if storage locking is unavailable.
                return true;
            }
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
