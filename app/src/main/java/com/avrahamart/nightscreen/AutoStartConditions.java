package com.avrahamart.nightscreen;

import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Shared gate for every automatic activation entry point. */
final class AutoStartConditions {
    private static final long USAGE_LOOKBACK_MS = 24L * 60L * 60L * 1000L;

    private AutoStartConditions() { }

    static boolean canActivate(Context context, MonitorSettingsSnapshot settings) {
        if (settings == null) return false;
        return canActivate(
                context,
                settings.autoStartOnlyWhenLocked,
                settings.preventAutoStartDuringUse,
                settings.preventAutoStartForSelectedApps,
                settings.excludedForegroundPackages);
    }

    static boolean canActivate(Context context, SharedPreferences prefs) {
        if (prefs == null) return true;
        Set<String> selectedApps = prefs.getStringSet(
                "excludedForegroundPackages", Collections.emptySet());
        return canActivate(
                context,
                prefs.getBoolean("autoStartOnlyWhenLocked", false),
                prefs.getBoolean("preventAutoStartDuringUse", false),
                prefs.getBoolean("pauseWhenExcludedAppActive", false),
                selectedApps == null ? Collections.emptySet() : selectedApps);
    }

    private static boolean canActivate(
            Context context,
            boolean onlyWhenLocked,
            boolean blockDuringAnyAppUse,
            boolean blockForSelectedApps,
            Set<String> selectedApps) {
        if (onlyWhenLocked && !isDeviceLocked(context)) return false;

        boolean selectedFilterActive =
                blockForSelectedApps && selectedApps != null && !selectedApps.isEmpty();
        if (!blockDuringAnyAppUse && !selectedFilterActive) return true;

        // Both protections are evaluated before launch. The all-apps option blocks
        // any non-launcher app; the selected-apps option blocks only package names
        // explicitly chosen by the user. No Activity is closed after launch.
        Set<String> foregroundPackages = getForegroundPackages(context);
        if (foregroundPackages == null) {
            // Fail closed when Usage Access cannot reliably identify the foreground app.
            return false;
        }

        if (blockDuringAnyAppUse) {
            String launcherPackage = getHomePackage(context);
            for (String packageName : foregroundPackages) {
                if (packageName == null
                        || packageName.isEmpty()
                        || context.getPackageName().equals(packageName)
                        || packageName.equals(launcherPackage)
                        || "com.android.systemui".equals(packageName)
                        || "android".equals(packageName)) {
                    continue;
                }
                return false;
            }
        }

        if (selectedFilterActive) {
            for (String packageName : selectedApps) {
                if (foregroundPackages.contains(packageName)) return false;
            }
        }
        return true;
    }

    static boolean isDeviceLocked(Context context) {
        KeyguardManager keyguard =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return keyguard != null && keyguard.isKeyguardLocked();
    }

    /**
     * Returns packages with at least one resumed activity according to UsageEvents.
     * A null result means the foreground state could not be determined.
     */
    private static Set<String> getForegroundPackages(Context context) {
        if (!hasUsageAccess(context)) return null;

        UsageStatsManager usage =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usage == null) return null;

        long now = System.currentTimeMillis();
        long bootTime = now - SystemClock.elapsedRealtime();
        long begin = Math.max(bootTime, now - USAGE_LOOKBACK_MS);

        try {
            UsageEvents events = usage.queryEvents(begin, now);
            if (events == null) return null;

            Map<String, Set<String>> resumedByPackage = new HashMap<>();
            UsageEvents.Event event = new UsageEvents.Event();

            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                String packageName = event.getPackageName();
                if (packageName == null || packageName.isEmpty()) continue;

                int type = event.getEventType();
                boolean foreground = type == UsageEvents.Event.MOVE_TO_FOREGROUND
                        || (Build.VERSION.SDK_INT >= 29
                        && type == UsageEvents.Event.ACTIVITY_RESUMED);
                boolean background = type == UsageEvents.Event.MOVE_TO_BACKGROUND
                        || (Build.VERSION.SDK_INT >= 29
                        && (type == UsageEvents.Event.ACTIVITY_PAUSED
                        || type == UsageEvents.Event.ACTIVITY_STOPPED));

                if (!foreground && !background) continue;

                String className = event.getClassName();
                if (className == null || className.isEmpty()) className = "*";

                if (foreground) {
                    Set<String> activities = resumedByPackage.get(packageName);
                    if (activities == null) {
                        activities = new HashSet<>();
                        resumedByPackage.put(packageName, activities);
                    }
                    activities.add(className);
                } else {
                    Set<String> activities = resumedByPackage.get(packageName);
                    if (activities != null) {
                        if ("*".equals(className)
                                || type == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                            activities.clear();
                        } else {
                            activities.remove(className);
                        }
                        if (activities.isEmpty()) resumedByPackage.remove(packageName);
                    }
                }
            }

            Set<String> foregroundPackages = new HashSet<>();
            for (Map.Entry<String, Set<String>> entry : resumedByPackage.entrySet()) {
                if (!entry.getValue().isEmpty()) foregroundPackages.add(entry.getKey());
            }
            return foregroundPackages;
        } catch (Throwable error) {
            android.util.Log.w("NightScreenAutoStart",
                    "Could not identify foreground apps from usage events.", error);
            return null;
        }
    }

    private static boolean hasUsageAccess(Context context) {
        try {
            AppOpsManager manager =
                    (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            return manager != null && manager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), context.getPackageName())
                    == AppOpsManager.MODE_ALLOWED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String getHomePackage(Context context) {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            PackageManager pm = context.getPackageManager();
            ResolveInfo info = pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
            return info == null || info.activityInfo == null
                    ? null : info.activityInfo.packageName;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
