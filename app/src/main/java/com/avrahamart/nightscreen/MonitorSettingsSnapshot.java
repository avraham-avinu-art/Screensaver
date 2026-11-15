package com.avrahamart.nightscreen;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

/** Fresh cross-process view of automatic-start settings and lock-cycle state. */
final class MonitorSettingsSnapshot {
    private static final Uri URI =
            Uri.parse("content://" + MonitorSettingsProvider.AUTHORITY);

    final boolean autoStartOnDeviceLock;
    final boolean autoStartOnBoot;
    final boolean autoStartOnWake;
    final boolean autoStartOnCharge;
    final boolean useAndroidDreamForCharging;
    final boolean autoStartOnlyWhenLocked;
    final boolean preventAutoStartDuringUse;
    final boolean wakeOnTrackChange;
    final boolean showPlayer;
    final int trackChangeWakeSeconds;
    final boolean deviceLockActivationConsumed;

    private MonitorSettingsSnapshot(Bundle data) {
        autoStartOnDeviceLock = data.getBoolean("autoStartOnDeviceLock", false);
        autoStartOnBoot = data.getBoolean("autoStartOnBoot", false);
        autoStartOnWake = data.getBoolean("autoStartOnWake", false);
        autoStartOnCharge = data.getBoolean("autoStartOnCharge", false);
        useAndroidDreamForCharging =
                data.getBoolean("useAndroidDreamForCharging", false);
        autoStartOnlyWhenLocked = data.getBoolean("autoStartOnlyWhenLocked", false);
        preventAutoStartDuringUse = data.getBoolean("preventAutoStartDuringUse", false);
        wakeOnTrackChange = data.getBoolean("wakeOnTrackChange", false);
        showPlayer = data.getBoolean("showPlayer", false);
        trackChangeWakeSeconds = data.getInt("trackChangeWakeSeconds", 5);
        deviceLockActivationConsumed =
                data.getBoolean("deviceLockActivationConsumed", false);
    }

    static MonitorSettingsSnapshot read(Context context) {
        Context app = context.getApplicationContext();
        try {
            Bundle data;
            if (isDefaultProcess(app)) {
                SharedPreferences prefs =
                        app.getSharedPreferences("settings", Context.MODE_PRIVATE);
                data = new Bundle();
                data.putBoolean("autoStartOnDeviceLock",
                        prefs.getBoolean("autoStartOnDeviceLock", false));
                data.putBoolean("autoStartOnBoot",
                        prefs.getBoolean("autoStartOnBoot", false));
                data.putBoolean("autoStartOnWake",
                        prefs.getBoolean("autoStartOnWake", false));
                data.putBoolean("autoStartOnCharge",
                        prefs.getBoolean("autoStartOnCharge", false));
                data.putBoolean("useAndroidDreamForCharging",
                        prefs.getBoolean("useAndroidDreamForCharging", false));
                data.putBoolean("autoStartOnlyWhenLocked",
                        prefs.getBoolean("autoStartOnlyWhenLocked", false));
                data.putBoolean("preventAutoStartDuringUse",
                        prefs.getBoolean("preventAutoStartDuringUse", false));
                data.putBoolean("wakeOnTrackChange",
                        prefs.getBoolean("wakeOnTrackChange", false));
                data.putBoolean("showPlayer", prefs.getBoolean("showPlayer", false));
                data.putInt("trackChangeWakeSeconds",
                        prefs.getInt("trackChangeWakeSeconds", 5));
                data.putBoolean("deviceLockActivationConsumed",
                        prefs.getBoolean("deviceLockActivationConsumed", false));
            } else {
                data = app.getContentResolver().call(
                        URI, MonitorSettingsProvider.METHOD_READ, null, null);
            }
            return data == null ? null : new MonitorSettingsSnapshot(data);
        } catch (Throwable error) {
            android.util.Log.e("NightScreenAutoStart",
                    "Unable to read automatic-start settings.", error);
            return null;
        }
    }

    static void setSuppressNextDeviceLockScreenOff(Context context, boolean enabled) {
        try {
            Bundle extras = new Bundle();
            extras.putBoolean("enabled", enabled);
            context.getContentResolver().call(
                    URI, MonitorSettingsProvider.METHOD_SET_SCREEN_OFF_SUPPRESS,
                    null, extras);
        } catch (Throwable error) {
            android.util.Log.e("NightScreenAutoStart",
                    "Unable to update the screen-off suppression marker.", error);
        }
    }

    static boolean claimSuppressNextDeviceLockScreenOff(Context context) {
        try {
            Bundle result = context.getContentResolver().call(
                    URI, MonitorSettingsProvider.METHOD_CLAIM_SCREEN_OFF_SUPPRESS,
                    null, null);
            return result != null && result.getBoolean("suppressed", false);
        } catch (Throwable error) {
            android.util.Log.e("NightScreenAutoStart",
                    "Unable to consume the screen-off suppression marker.", error);
            return false;
        }
    }

    static boolean claimDeviceLockActivation(Context context) {
        try {
            Bundle result = context.getContentResolver().call(
                    URI, MonitorSettingsProvider.METHOD_CLAIM_LOCK, null, null);
            return result != null && result.getBoolean("claimed", false);
        } catch (Throwable error) {
            android.util.Log.e("NightScreenAutoStart",
                    "Unable to claim the current device-lock cycle.", error);
            return false;
        }
    }

    static void resetDeviceLockActivation(Context context) {
        try {
            context.getContentResolver().call(
                    URI, MonitorSettingsProvider.METHOD_RESET_LOCK, null, null);
        } catch (Throwable error) {
            android.util.Log.e("NightScreenAutoStart",
                    "Unable to reset the current device-lock cycle.", error);
        }
    }

    private static boolean isDefaultProcess(Context context) {
        String processName = null;
        if (Build.VERSION.SDK_INT >= 28) {
            processName = Application.getProcessName();
        } else {
            ActivityManager manager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                try {
                    java.util.List<ActivityManager.RunningAppProcessInfo> processes =
                            manager.getRunningAppProcesses();
                    if (processes != null) {
                        for (ActivityManager.RunningAppProcessInfo process : processes) {
                            if (process.pid == Process.myPid()) {
                                processName = process.processName;
                                break;
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return context.getPackageName().equals(processName);
    }
}
