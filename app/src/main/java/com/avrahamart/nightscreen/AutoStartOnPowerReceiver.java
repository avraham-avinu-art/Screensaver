package com.avrahamart.nightscreen;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public final class AutoStartOnPowerReceiver extends BroadcastReceiver {
    private static final String TAG = "NightScreenAutoStart";
    private static final long DEFAULT_LOCK_STATE_WAIT_MS = 30_000L;
    private static final long MAX_LOCK_STATE_WAIT_MS = 10 * 60_000L;
    private static final long LOCK_STATE_POLL_MS = 250L;
    private static final long LOCK_STATE_INITIAL_DELAY_MS = 350L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingLockCheck;
    private PowerManager.WakeLock pendingLockWakeLock;
    private long lockCheckDeadlineElapsedRealtime;

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        MonitorSettingsSnapshot state = MonitorSettingsSnapshot.read(context);
        if (state == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_USER_PRESENT.equals(action)) {
            cancelPendingLockCheck();
            MonitorSettingsSnapshot.resetDeviceLockStart(context);
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            onScreenOff(context, state);
        } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
            onScreenOn(context, state);
        } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            onPowerConnected(context, state);
        }
    }

    private void onScreenOff(Context context, MonitorSettingsSnapshot state) {
        boolean enabled = AutoStartPolicy.isEnabledForAction(
                Intent.ACTION_SCREEN_OFF, state.autoStartOnDeviceLock, false, false, false, false);
        if (!AutoStartPolicy.shouldLaunchNewSession(
                enabled, hasScreensaverSessionInLockCycle(context, state))) return;
        if (!LockScreenMonitorService.isLockMonitoringActive()
                || state.deviceLockStartConsumed || !canLaunchFromBackground(context)) return;
        beginDeviceLockCheck(context.getApplicationContext());
    }

    private void onScreenOn(Context context, MonitorSettingsSnapshot state) {
        if (pendingLockCheck != null && !isDeviceLocked(context)) cancelPendingLockCheck();
        boolean enabled = AutoStartPolicy.isEnabledForAction(
                Intent.ACTION_SCREEN_ON, false, false, state.autoStartOnWake, false, false);
        if (!AutoStartPolicy.shouldLaunchNewSession(
                enabled, hasScreensaverSessionInLockCycle(context, state))) return;
        if (!LockScreenMonitorService.isLockMonitoringActive()
                || !canLaunchFromBackground(context)) return;
        AutoStartActivityLauncher.launch(context, AutoStartPolicy.Trigger.DEVICE_WAKE);
    }

    private void onPowerConnected(Context context, MonitorSettingsSnapshot state) {
        boolean enabled = AutoStartPolicy.isEnabledForAction(
                Intent.ACTION_POWER_CONNECTED, false, false, false,
                state.autoStartOnCharge, state.useAndroidDreamForCharging);
        if (!AutoStartPolicy.shouldLaunchNewSession(
                enabled, hasScreensaverSessionInLockCycle(context, state))) return;
        if (!canLaunchFromBackground(context) || !claimPowerConnectedEvent(context)) return;
        AutoStartActivityLauncher.launch(context, AutoStartPolicy.Trigger.CHARGER);
    }

    private void beginDeviceLockCheck(Context appContext) {
        if (pendingLockCheck != null) return;
        MonitorSettingsSnapshot state = MonitorSettingsSnapshot.read(appContext);
        if (state == null || !state.autoStartOnDeviceLock
                || !LockScreenMonitorService.isLockMonitoringActive()
                || state.deviceLockStartConsumed
                || hasScreensaverSessionInLockCycle(appContext, state)) return;

        KeyguardManager keyguard =
                (KeyguardManager) appContext.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguard == null || !keyguard.isDeviceSecure()) return;

        PowerManager power =
                (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        long waitWindowMs = getDeviceLockWaitWindowMs(appContext);
        try {
            if (power != null) {
                pendingLockWakeLock = power.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK, "NightScreen:DeviceLockCheck");
                pendingLockWakeLock.acquire(waitWindowMs + 2_000L);
            }
        } catch (Throwable error) {
            pendingLockWakeLock = null;
            Log.w(TAG, "Unable to hold CPU while checking device lock", error);
        }

        lockCheckDeadlineElapsedRealtime = SystemClock.elapsedRealtime() + waitWindowMs;
        pendingLockCheck = new Runnable() {
            @Override public void run() {
                MonitorSettingsSnapshot current = MonitorSettingsSnapshot.read(appContext);
                if (current == null || !current.autoStartOnDeviceLock
                        || !LockScreenMonitorService.isLockMonitoringActive()
                        || current.deviceLockStartConsumed
                        || hasScreensaverSessionInLockCycle(appContext, current)
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

    private long getDeviceLockWaitWindowMs(Context context) {
        long waitMs = DEFAULT_LOCK_STATE_WAIT_MS;
        try {
            long lockAfter = Settings.Secure.getLong(
                    context.getContentResolver(), "lock_screen_lock_after_timeout", 0L);
            if (lockAfter > 0L) waitMs = Math.max(waitMs, lockAfter + 5_000L);
        } catch (Throwable ignored) {
        }
        return Math.min(MAX_LOCK_STATE_WAIT_MS, waitMs);
    }

    private boolean isDeviceLocked(Context context) {
        KeyguardManager keyguard =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        try {
            return keyguard != null && keyguard.isDeviceLocked();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void launchOnDeviceLock(Context context) {
        MonitorSettingsSnapshot state = MonitorSettingsSnapshot.read(context);
        if (state == null || !state.autoStartOnDeviceLock
                || !LockScreenMonitorService.isLockMonitoringActive()
                || state.deviceLockStartConsumed
                || hasScreensaverSessionInLockCycle(context, state)
                || !isDeviceLocked(context)
                || !canLaunchFromBackground(context)) return;
        if (!MonitorSettingsSnapshot.claimDeviceLockStart(context)) return;
        if (!AutoStartActivityLauncher.launch(context, AutoStartPolicy.Trigger.DEVICE_LOCK)) {
            MonitorSettingsSnapshot.resetDeviceLockStart(context);
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
        synchronized (AutoStartOnPowerReceiver.class) {
            File guard = new File(context.getNoBackupFilesDir(), "power_connected_guard.bin");
            long now = SystemClock.elapsedRealtime();
            try (RandomAccessFile file = new RandomAccessFile(guard, "rw");
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
            } catch (Throwable error) {
                Log.w(TAG, "Unable to serialize charger event", error);
                return true;
            }
        }
    }

    private boolean hasScreensaverSessionInLockCycle(
            Context context, MonitorSettingsSnapshot state) {
        if (!state.mainActivitySessionAlive || state.mainActivitySessionPid <= 0) return false;
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;
        try {
            java.util.List<ActivityManager.RunningAppProcessInfo> processes =
                    manager.getRunningAppProcesses();
            if (processes == null) return false;
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (process.pid != state.mainActivitySessionPid
                        || !context.getPackageName().equals(process.processName)) continue;
                return state.mainActivityResumed
                        || state.mainActivityPausedByDeviceLock
                        || process.importance
                            <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private boolean canLaunchFromBackground(Context context) {
        return Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(context);
    }
}
