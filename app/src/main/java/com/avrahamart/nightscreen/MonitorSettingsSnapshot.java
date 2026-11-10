package com.avrahamart.nightscreen;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

final class MonitorSettingsSnapshot {
    private static final Uri URI = Uri.parse("content://" + MonitorSettingsProvider.AUTHORITY);
    final boolean autoStartOnDeviceLock, autoStartOnBoot, autoStartOnWake, autoStartOnCharge;
    final boolean useAndroidDreamForCharging, deviceLockStartConsumed, continueOnLockScreen;
    final boolean mainActivitySessionAlive, mainActivityResumed, mainActivityPausedByDeviceLock;
    final boolean mainActivityWaitingForScreenWake;
    final int mainActivitySessionPid;

    private MonitorSettingsSnapshot(Bundle b) {
        autoStartOnDeviceLock = b.getBoolean("autoStartOnDeviceLock", false);
        autoStartOnBoot = b.getBoolean("autoStartOnBoot", false);
        autoStartOnWake = b.getBoolean("autoStartOnWake", false);
        autoStartOnCharge = b.getBoolean("autoStartOnCharge", false);
        useAndroidDreamForCharging = b.getBoolean("useAndroidDreamForCharging", true);
        deviceLockStartConsumed = b.getBoolean("deviceLockStartConsumed", false);
        continueOnLockScreen = b.getBoolean("continueOnLockScreen", false);
        mainActivitySessionAlive = b.getBoolean("mainActivitySessionAlive", false);
        mainActivityResumed = b.getBoolean("mainActivityResumed", false);
        mainActivityPausedByDeviceLock = b.getBoolean("mainActivityPausedByDeviceLock", false);
        mainActivityWaitingForScreenWake = b.getBoolean("mainActivityWaitingForScreenWake", false);
        mainActivitySessionPid = b.getInt("mainActivitySessionPid", -1);
    }

    static MonitorSettingsSnapshot read(Context context) {
        Context app = context.getApplicationContext();
        try {
            Bundle b = isDefaultProcess(app) ? readLocalPreferences(app)
                    : app.getContentResolver().call(URI, MonitorSettingsProvider.METHOD_READ, null, null);
            return b == null ? null : new MonitorSettingsSnapshot(b);
        } catch (Throwable e) {
            android.util.Log.e("NightScreenAutoStart", "Unable to read monitor state.", e);
            return null;
        }
    }

    private static boolean isDefaultProcess(Context context) {
        String name = null;
        if (Build.VERSION.SDK_INT >= 28) name = Application.getProcessName();
        else {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) try {
                java.util.List<ActivityManager.RunningAppProcessInfo> list = manager.getRunningAppProcesses();
                if (list != null) for (ActivityManager.RunningAppProcessInfo p : list) {
                    if (p.pid == Process.myPid()) { name = p.processName; break; }
                }
            } catch (Throwable ignored) { }
        }
        return context.getPackageName().equals(name);
    }

    private static Bundle readLocalPreferences(Context c) {
        SharedPreferences p = c.getSharedPreferences("settings", Context.MODE_PRIVATE);
        Bundle b = new Bundle();
        b.putBoolean("autoStartOnDeviceLock", p.getBoolean("autoStartOnDeviceLock", false));
        b.putBoolean("autoStartOnBoot", p.getBoolean("autoStartOnBoot", false));
        b.putBoolean("autoStartOnWake", p.getBoolean("autoStartOnWake", false));
        b.putBoolean("autoStartOnCharge", p.getBoolean("autoStartOnCharge", false));
        b.putBoolean("useAndroidDreamForCharging", p.getBoolean("useAndroidDreamForCharging", true));
        b.putBoolean("deviceLockStartConsumed", p.getBoolean("deviceLockStartConsumed", false));
        b.putBoolean("continueOnLockScreen", p.getBoolean("continueOnLockScreen", false));
        b.putBoolean("mainActivitySessionAlive", p.getBoolean("mainActivitySessionAlive", false));
        b.putBoolean("mainActivityResumed", p.getBoolean("mainActivityResumed", false));
        b.putBoolean("mainActivityPausedByDeviceLock", p.getBoolean("mainActivityPausedByDeviceLock", false));
        b.putBoolean("mainActivityWaitingForScreenWake", p.getBoolean("mainActivityWaitingForScreenWake", false));
        b.putInt("mainActivitySessionPid", p.getInt("mainActivitySessionPid", -1));
        return b;
    }

    boolean needsMonitorService() {
        return AutoStartPolicy.needsMonitorService(
                autoStartOnDeviceLock, autoStartOnBoot, autoStartOnWake, autoStartOnCharge);
    }

    static boolean claimDeviceLockStart(Context c) {
        try {
            Bundle b = c.getContentResolver().call(URI, MonitorSettingsProvider.METHOD_CLAIM_LOCK, null, null);
            return b != null && b.getBoolean("claimed", false);
        } catch (Throwable e) {
            android.util.Log.e("NightScreenAutoStart", "Unable to claim lock cycle.", e);
            return false;
        }
    }

    static void resetDeviceLockStart(Context c) {
        try {
            c.getContentResolver().call(URI, MonitorSettingsProvider.METHOD_RESET_LOCK, null, null);
        } catch (Throwable e) {
            android.util.Log.w("NightScreenAutoStart", "Unable to reset lock cycle.", e);
        }
    }
}
