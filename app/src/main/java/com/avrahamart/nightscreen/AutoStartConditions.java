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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Shared gate for every automatic activation entry point. */
final class AutoStartConditions {
    private AutoStartConditions() { }

    static boolean canActivate(Context context, MonitorSettingsSnapshot settings) {
        if (settings == null) return false;
        if (settings.autoStartOnlyWhenLocked && !isDeviceLocked(context)) return false;
        return !settings.preventAutoStartDuringUse || !isAnotherAppInForeground(context);
    }

    static boolean canActivate(Context context, SharedPreferences prefs) {
        if (prefs == null) return true;
        if (prefs.getBoolean("autoStartOnlyWhenLocked", false)
                && !isDeviceLocked(context)) return false;
        return !prefs.getBoolean("preventAutoStartDuringUse", false)
                || !isAnotherAppInForeground(context);
    }

    static boolean isDeviceLocked(Context context) {
        KeyguardManager keyguard =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return keyguard != null && keyguard.isKeyguardLocked();
    }

    /**
     * Reconstructs currently-resumed activities from UsageEvents. This is a
     * single synchronous query at activation time, not a polling loop or delay.
     * The launcher and this app are excluded; a non-launcher foreground app
     * blocks the automatic start.
     */
    static boolean isAnotherAppInForeground(Context context) {
        // When the user enables this protection but revokes Usage Access,
        // fail closed instead of launching and interrupting active use.
        if (!hasUsageAccess(context)) return true;

        UsageStatsManager usage =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usage == null) return true;

        long now = System.currentTimeMillis();
        long bootTime = now - SystemClock.elapsedRealtime();
        long begin = Math.max(bootTime, now - 24L * 60L * 60L * 1000L);

        try {
            UsageEvents events = usage.queryEvents(begin, now);
            if (events == null) return true;

            String launcherPackage = getHomePackage(context);
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

            for (Map.Entry<String, Set<String>> entry : resumedByPackage.entrySet()) {
                String packageName = entry.getKey();
                if (entry.getValue().isEmpty()
                        || context.getPackageName().equals(packageName)
                        || packageName.equals(launcherPackage)
                        || "com.android.systemui".equals(packageName)
                        || "android".equals(packageName)) continue;
                return true;
            }
        } catch (Throwable error) {
            android.util.Log.w("NightScreenAutoStart",
                    "Could not identify the foreground app from usage events.", error);
            return true;
        }
        return false;
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
