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
import android.os.SystemClock;
import android.provider.Settings;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Handles independent automatic-start events. A screen-off event is only a
 * trigger to inspect the real device-lock state; it is never treated as a lock
 * by itself.
 */
public final class AutoStartOnPowerReceiver extends BroadcastReceiver {
    private static final long DEFAULT_LOCK_STATE_WAIT_MS = 30_000L;
    private static final long MAX_LOCK_STATE_WAIT_MS = 10 * 60_000L;
    private static final long LOCK_STATE_POLL_MS = 250L;
    private static final long LOCK_STATE_INITIAL_DELAY_MS = 350L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingLockCheck;
    private PowerManager.WakeLock pendingLockWakeLock;
    private long lockCheckDeadlineElapsedRealtime;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        SharedPreferences prefs =
                context.getSharedPreferences("settings", Context.MODE_PRIVATE);

        if (Intent.ACTION_USER_PRESENT.equals(action)) {
            MainActivity.clearPausedByDeviceLockState();
            cancelPendingLockCheck();
            // Rearm only after Android reports that the user has actually unlocked.
            prefs.edit().putBoolean("deviceLockStartConsumed", false).apply();
            return;
        }

        if (Intent.ACTION_SCREEN_ON.equals(action)) {
            // A pending lock delay is no longer relevant if the display woke while
            // the device is still unlocked. If it is already locked, keep checking.
            if (pendingLockCheck != null && !isDeviceLocked(context)) {
                cancelPendingLockCheck();
            }

            if (prefs.getBoolean("autoStartOnWake", false)
                    && LockScreenMonitorService.isLockMonitoringActive()
                    && !isMainActivityActivelyResumed(context)
                    && canLaunchFromBackground(context)) {
                Intent launch = new Intent(context, MainActivity.class);
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                launch.putExtra(MainActivity.EXTRA_OPENED_BY_WAKE, true);
                try {
                    context.startActivity(launch);
                } catch (Throwable ignored) {
                    // Respect device-specific background-launch restrictions.
                }
            }
            return;
        }

        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            // Android Dream is an alternative route. In that mode the system
            // starts ScreensaverDreamService; this receiver must not start a
            // second Activity for the same charging event.
            if (prefs.getBoolean("useAndroidDreamForCharging", true)
                    || !prefs.getBoolean("autoStartOnCharge", false)) return;
            if (!canLaunchFromBackground(context)) return;
            if (!claimPowerConnectedEvent(context)) return;

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
                && LockScreenMonitorService.isLockMonitoringActive()
                && !prefs.getBoolean("deviceLockStartConsumed", false)
                && canLaunchFromBackground(context)) {
            beginDeviceLockCheck(context.getApplicationContext());
        }
    }

    private void beginDeviceLockCheck(Context appContext) {
        if (pendingLockCheck != null) return;

        SharedPreferences prefs = appContext.getSharedPreferences(
                "settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("autoStartOnDeviceLock", false)
                || !LockScreenMonitorService.isLockMonitoringActive()
                || prefs.getBoolean("deviceLockStartConsumed", false)) return;

        KeyguardManager keyguardManager =
                (KeyguardManager) appContext.getSystemService(Context.KEYGUARD_SERVICE);
        // A swipe-only keyguard is not a device lock: Android considers the
        // device unlocked because no authentication is required.
        if (keyguardManager == null || !keyguardManager.isDeviceSecure()) return;

        PowerManager powerManager =
                (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        long waitWindowMs = getDeviceLockWaitWindowMs(appContext);
        try {
            if (powerManager != null) {
                pendingLockWakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "NightScreen:DeviceLockCheck");
                pendingLockWakeLock.acquire(waitWindowMs + 2_000L);
            }
        } catch (Throwable ignored) {
            pendingLockWakeLock = null;
        }

        lockCheckDeadlineElapsedRealtime =
                SystemClock.elapsedRealtime() + waitWindowMs;
        pendingLockCheck = new Runnable() {
            @Override
            public void run() {
                SharedPreferences currentPrefs = appContext.getSharedPreferences(
                        "settings", Context.MODE_PRIVATE);
                if (!currentPrefs.getBoolean("autoStartOnDeviceLock", false)
                        || !LockScreenMonitorService.isLockMonitoringActive()
                        || currentPrefs.getBoolean("deviceLockStartConsumed", false)
                        || !canLaunchFromBackground(appContext)) {
                    finishPendingLockCheck();
                    return;
                }

                if (isDeviceLocked(appContext)) {
                    launchOnDeviceLock(appContext);
                    finishPendingLockCheck();
                    return;
                }

                if (SystemClock.elapsedRealtime() >= lockCheckDeadlineElapsedRealtime) {
                    finishPendingLockCheck();
                    return;
                }
                mainHandler.postDelayed(this, LOCK_STATE_POLL_MS);
            }
        };
        mainHandler.postDelayed(pendingLockCheck, LOCK_STATE_INITIAL_DELAY_MS);
    }

    /**
     * Android may delay applying Keyguard after the display turns off. Read the
     * configured lock-after-screen-off delay when the platform exposes it and
     * keep the bounded monitor alive long enough to observe that transition.
     */
    private long getDeviceLockWaitWindowMs(Context context) {
        long waitMs = DEFAULT_LOCK_STATE_WAIT_MS;
        try {
            long lockAfterTimeoutMs = Settings.Secure.getLong(
                    context.getContentResolver(),
                    "lock_screen_lock_after_timeout",
                    0L);
            if (lockAfterTimeoutMs > 0L) {
                waitMs = Math.max(waitMs, lockAfterTimeoutMs + 5_000L);
            }
        } catch (Throwable ignored) {
            // Some Android builds do not expose this secure setting to apps.
        }
        return Math.min(MAX_LOCK_STATE_WAIT_MS, waitMs);
    }

    private boolean isDeviceLocked(Context context) {
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        try {
            return keyguardManager != null && keyguardManager.isDeviceLocked();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void launchOnDeviceLock(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("autoStartOnDeviceLock", false)
                || !LockScreenMonitorService.isLockMonitoringActive()
                || prefs.getBoolean("deviceLockStartConsumed", false)
                || !isDeviceLocked(context)
                || !canLaunchFromBackground(context)) return;

        // Consume this actual locked-state transition once. Unlike the previous
        // implementation, do not reject the launch merely because the app process
        // is visible: MainActivity may be paused behind the lock screen and must
        // receive this intent so it can display above Keyguard.
        synchronized (AutoStartOnPowerReceiver.class) {
            if (prefs.getBoolean("deviceLockStartConsumed", false)) return;
            if (!prefs.edit().putBoolean("deviceLockStartConsumed", true).commit()) return;
        }

        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(MainActivity.EXTRA_OPENED_BY_DEVICE_LOCK, true);
        try {
            context.startActivity(launch);
        } catch (Throwable ignored) {
            // Allow a later retry if Android rejected this launch.
            prefs.edit().putBoolean("deviceLockStartConsumed", false).apply();
        }
    }

    void cancelPendingLockCheck() {
        if (pendingLockCheck != null) {
            mainHandler.removeCallbacks(pendingLockCheck);
            pendingLockCheck = null;
        }
        if (pendingLockWakeLock != null) {
            try {
                if (pendingLockWakeLock.isHeld()) pendingLockWakeLock.release();
            } catch (Throwable ignored) {
            }
            pendingLockWakeLock = null;
        }
    }

    private void finishPendingLockCheck() {
        cancelPendingLockCheck();
    }

    private boolean claimPowerConnectedEvent(Context context) {
        // The receiver can exist in more than one app process. A short file lock
        // prevents duplicate launches for the same plug-in event.
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

    private boolean isMainActivityActivelyResumed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("mainActivityResumed", false)
                && isMainActivityVisible(context);
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
                    return process.importance
                            <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private boolean canLaunchFromBackground(Context context) {
        return Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(context);
    }
}
