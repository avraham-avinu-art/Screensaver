package com.avrahamart.nightscreen;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * Single-process access point for the few preferences read by services that
 * run in Android's isolated :lockmonitor and :notifications processes.
 */
public final class MonitorSettingsProvider extends ContentProvider {
    static final String AUTHORITY = "com.avrahamart.nightscreen.monitorstate";
    static final String METHOD_READ = "readAutoStartSettings";
    static final String METHOD_CLAIM_LOCK = "claimDeviceLockActivation";
    static final String METHOD_RESET_LOCK = "resetDeviceLockActivation";
    static final String METHOD_SET_SCREEN_OFF_SUPPRESS = "setScreenOffAutoStartSuppress";
    static final String METHOD_CLAIM_SCREEN_OFF_SUPPRESS = "claimScreenOffAutoStartSuppress";
    private static final Object LOCK_MUTEX = new Object();

    @Override
    public boolean onCreate() {
        return getContext() != null;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Context context = getContext();
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

        if (METHOD_READ.equals(method)) {
            Bundle result = new Bundle();
            result.putBoolean("autoStartOnDeviceLock",
                    prefs.getBoolean("autoStartOnDeviceLock", false));
            result.putBoolean("autoStartOnBoot",
                    prefs.getBoolean("autoStartOnBoot", false));
            result.putBoolean("autoStartOnWake",
                    prefs.getBoolean("autoStartOnWake", false));
            result.putBoolean("autoStartOnCharge",
                    prefs.getBoolean("autoStartOnCharge", false));
            result.putBoolean("autoStartOnIdle",
                    prefs.getBoolean("autoStartOnIdle", false));
            result.putBoolean("autoStartPaused",
                    prefs.getBoolean("autoStartPaused", false));
            result.putInt("idleWaitSeconds", prefs.getInt("idleWaitSeconds", 30));
            result.putBoolean("useAndroidDreamForCharging",
                    prefs.getBoolean("useAndroidDreamForCharging", false));
            result.putBoolean("autoStartOnlyWhenLocked",
                    prefs.getBoolean("autoStartOnlyWhenLocked", false));
            result.putBoolean("preventAutoStartDuringUse",
                    prefs.getBoolean("preventAutoStartDuringUse", false));
            result.putBoolean("pauseWhenExcludedAppActive",
                    prefs.getBoolean("pauseWhenExcludedAppActive", false));
            Set<String> selectedApps = prefs.getStringSet(
                    "excludedForegroundPackages", Collections.emptySet());
            result.putStringArrayList("excludedForegroundPackages",
                    new ArrayList<>(selectedApps == null
                            ? Collections.emptySet() : selectedApps));
            result.putBoolean("wakeOnTrackChange",
                    prefs.getBoolean("wakeOnTrackChange", false));
            result.putBoolean("showPlayer",
                    prefs.getBoolean("showPlayer", false));
            result.putBoolean("deviceLockActivationConsumed",
                    prefs.getBoolean("deviceLockActivationConsumed", false));
            return result;
        }

        if (METHOD_CLAIM_LOCK.equals(method)) {
            synchronized (LOCK_MUTEX) {
                boolean consumed = prefs.getBoolean("deviceLockActivationConsumed", false);
                if (consumed) return resultBoolean("claimed", false);
                boolean saved = prefs.edit()
                        .putBoolean("deviceLockActivationConsumed", true)
                        .commit();
                return resultBoolean("claimed", saved);
            }
        }

        if (METHOD_RESET_LOCK.equals(method)) {
            synchronized (LOCK_MUTEX) {
                boolean saved = prefs.edit()
                        .putBoolean("deviceLockActivationConsumed", false)
                        .commit();
                return resultBoolean("saved", saved);
            }
        }

        if (METHOD_SET_SCREEN_OFF_SUPPRESS.equals(method)) {
            boolean enabled = extras != null && extras.getBoolean("enabled", false);
            boolean saved = prefs.edit()
                    .putBoolean("suppressNextDeviceLockScreenOff", enabled)
                    .commit();
            return resultBoolean("saved", saved);
        }

        if (METHOD_CLAIM_SCREEN_OFF_SUPPRESS.equals(method)) {
            synchronized (LOCK_MUTEX) {
                boolean pending = prefs.getBoolean("suppressNextDeviceLockScreenOff", false);
                if (!pending) return resultBoolean("suppressed", false);
                boolean saved = prefs.edit()
                        .remove("suppressNextDeviceLockScreenOff")
                        .commit();
                return resultBoolean("suppressed", saved);
            }
        }

        return super.call(method, arg, extras);
    }

    private Bundle resultBoolean(String key, boolean value) {
        Bundle result = new Bundle();
        result.putBoolean(key, value);
        return result;
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection,
                                String[] selectionArgs) { return 0; }
}
