package com.avrahamart.nightscreen;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

final class MonitorSettingsSnapshot {
    private static final Uri URI = Uri.parse("content://" + MonitorSettingsProvider.AUTHORITY);
    final boolean autoStartOnDeviceLock, autoStartOnWake, autoStartOnCharge;
    final boolean useAndroidDreamForCharging, deviceLockStartConsumed;
    final boolean mainActivitySessionAlive, mainActivityResumed, mainActivityPausedByDeviceLock;
    final int mainActivitySessionPid;

    private MonitorSettingsSnapshot(Bundle b) {
        autoStartOnDeviceLock = b.getBoolean("autoStartOnDeviceLock", false);
        autoStartOnWake = b.getBoolean("autoStartOnWake", false);
        autoStartOnCharge = b.getBoolean("autoStartOnCharge", false);
        useAndroidDreamForCharging = b.getBoolean("useAndroidDreamForCharging", true);
        deviceLockStartConsumed = b.getBoolean("deviceLockStartConsumed", false);
        mainActivitySessionAlive = b.getBoolean("mainActivitySessionAlive", false);
        mainActivityResumed = b.getBoolean("mainActivityResumed", false);
        mainActivityPausedByDeviceLock = b.getBoolean("mainActivityPausedByDeviceLock", false);
        mainActivitySessionPid = b.getInt("mainActivitySessionPid", -1);
    }

    static MonitorSettingsSnapshot read(Context c) {
        try {
            Bundle b = c.getContentResolver().call(URI, MonitorSettingsProvider.METHOD_READ, null, null);
            return b == null ? null : new MonitorSettingsSnapshot(b);
        } catch (Throwable e) {
            android.util.Log.e("NightScreenAutoStart", "Cannot read monitor state.", e);
            return null;
        }
    }

    boolean needsMonitorService() {
        return AutoStartPolicy.needsMonitorService(autoStartOnDeviceLock, autoStartOnWake,
                autoStartOnCharge, useAndroidDreamForCharging);
    }

    static boolean claimDeviceLockStart(Context c) {
        try {
            Bundle b = c.getContentResolver().call(
                    URI, MonitorSettingsProvider.METHOD_CLAIM_LOCK, null, null);
            return b != null && b.getBoolean("claimed", false);
        } catch (Throwable e) {
            android.util.Log.e("NightScreenAutoStart", "Cannot claim device-lock start.", e);
            return false;
        }
    }

    static void resetDeviceLockStart(Context c) {
        try {
            c.getContentResolver().call(URI, MonitorSettingsProvider.METHOD_RESET_LOCK, null, null);
        } catch (Throwable e) {
            android.util.Log.w("NightScreenAutoStart", "Cannot reset device-lock marker.", e);
        }
    }
}
