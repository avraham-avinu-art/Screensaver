package com.avrahamart.nightscreen;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public final class MonitorSettingsProvider extends ContentProvider {
    static final String AUTHORITY = "com.avrahamart.nightscreen.monitorstate";
    static final String METHOD_READ = "readMonitorState";
    static final String METHOD_CLAIM_LOCK = "claimDeviceLockStart";
    static final String METHOD_RESET_LOCK = "resetDeviceLockStart";
    private static final Object LOCK_MUTEX = new Object();

    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        if (getContext() == null) return null;
        SharedPreferences p = getContext().getSharedPreferences(
                "settings", android.content.Context.MODE_PRIVATE);
        if (METHOD_READ.equals(method)) {
            Bundle b = new Bundle();
            b.putBoolean("autoStartOnDeviceLock", p.getBoolean("autoStartOnDeviceLock", false));
            b.putBoolean("autoStartOnBoot", p.getBoolean("autoStartOnBoot", false));
            b.putBoolean("autoStartOnWake", p.getBoolean("autoStartOnWake", false));
            b.putBoolean("autoStartOnCharge", p.getBoolean("autoStartOnCharge", false));
            b.putBoolean("useAndroidDreamForCharging", p.getBoolean("useAndroidDreamForCharging", true));
            b.putBoolean("deviceLockStartConsumed", p.getBoolean("deviceLockStartConsumed", false));
            b.putBoolean("mainActivitySessionAlive", p.getBoolean("mainActivitySessionAlive", false));
            b.putBoolean("mainActivityResumed", p.getBoolean("mainActivityResumed", false));
            b.putBoolean("mainActivityPausedByDeviceLock", p.getBoolean("mainActivityPausedByDeviceLock", false));
            b.putBoolean("mainActivityWaitingForScreenWake", p.getBoolean("mainActivityWaitingForScreenWake", false));
            b.putBoolean("continueOnLockScreen", p.getBoolean("continueOnLockScreen", false));
            b.putInt("mainActivitySessionPid", p.getInt("mainActivitySessionPid", -1));
            return b;
        }
        if (METHOD_CLAIM_LOCK.equals(method)) {
            synchronized (LOCK_MUTEX) {
                Bundle b = new Bundle();
                boolean claimed = !p.getBoolean("deviceLockStartConsumed", false)
                        && p.edit().putBoolean("deviceLockStartConsumed", true).commit();
                b.putBoolean("claimed", claimed);
                return b;
            }
        }
        if (METHOD_RESET_LOCK.equals(method)) {
            synchronized (LOCK_MUTEX) {
                Bundle b = new Bundle();
                b.putBoolean("saved", p.edit()
                        .putBoolean("deviceLockStartConsumed", false)
                        .putBoolean("mainActivityPausedByDeviceLock", false)
                        .commit());
                return b;
            }
        }
        return super.call(method, arg, extras);
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
