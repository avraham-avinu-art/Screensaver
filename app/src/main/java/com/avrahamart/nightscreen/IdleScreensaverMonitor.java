package com.avrahamart.nightscreen;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;

import java.util.HashMap;
import java.util.Map;

/** Uses foreground-app events only; it never observes taps, gestures or window contents. */
final class IdleScreensaverMonitor {
    private static final long POLL_INTERVAL_MS = 2_000L;
    private static final long EVENT_LOOKBACK_MS = 24L * 60L * 60L * 1000L;
    private static final long LAUNCH_RETRY_COOLDOWN_MS = 10_000L;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Map<String, Long>> resumedActivities = new HashMap<>();
    private final Runnable pollRunnable = this::poll;

    private UsageStatsManager usageStatsManager;
    private String launcherPackage;
    private String foregroundPackage;
    private long lastUsageQueryWallTime;
    private long idleSinceElapsed = -1L;
    private long lastLaunchAttemptElapsed;
    private boolean running;
    private boolean launchPending;

    IdleScreensaverMonitor(Context context) {
        this.context = context.getApplicationContext();
    }

    void start() {
        if (running || !hasUsageAccess()) return;
        usageStatsManager = (UsageStatsManager)
                context.getSystemService(Context.USAGE_STATS_SERVICE);
        launcherPackage = resolveLauncherPackage();
        if (usageStatsManager == null || launcherPackage == null) return;

        running = true;
        resumedActivities.clear();
        foregroundPackage = null;
        idleSinceElapsed = -1L;
        launchPending = false;

        long nowWall = System.currentTimeMillis();
        long bootTime = Math.max(0L, nowWall - SystemClock.elapsedRealtime());
        long begin = Math.max(bootTime, nowWall - EVENT_LOOKBACK_MS);
        if (!readUsageEvents(begin, nowWall)) {
            running = false;
            return;
        }
        lastUsageQueryWallTime = nowWall;
        updateForegroundState(nowWall, true);
        handler.removeCallbacks(pollRunnable);
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    void stop() {
        running = false;
        handler.removeCallbacks(pollRunnable);
        resumedActivities.clear();
        foregroundPackage = null;
        idleSinceElapsed = -1L;
        launchPending = false;
    }

    private void poll() {
        if (!running) return;
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("autoStartOnIdle", false) || !hasUsageAccess()) {
            stop();
            return;
        }

        long nowWall = System.currentTimeMillis();
        long bootTime = Math.max(0L, nowWall - SystemClock.elapsedRealtime());
        long begin = Math.max(bootTime, Math.max(0L, lastUsageQueryWallTime - 1_000L));
        if (nowWall > begin && readUsageEvents(begin, nowWall)) {
            lastUsageQueryWallTime = nowWall;
            updateForegroundState(nowWall, false);
            maybeLaunchFromIdle(prefs);
        }
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private boolean readUsageEvents(long begin, long end) {
        try {
            UsageEvents events = usageStatsManager.queryEvents(begin, end);
            if (events == null) return false;
            UsageEvents.Event event = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                String packageName = event.getPackageName();
                if (packageName == null || packageName.isEmpty()) continue;

                int type = event.getEventType();
                boolean resumed = type == UsageEvents.Event.MOVE_TO_FOREGROUND
                        || (Build.VERSION.SDK_INT >= 29
                        && type == UsageEvents.Event.ACTIVITY_RESUMED);
                boolean paused = type == UsageEvents.Event.MOVE_TO_BACKGROUND
                        || (Build.VERSION.SDK_INT >= 29
                        && (type == UsageEvents.Event.ACTIVITY_PAUSED
                        || type == UsageEvents.Event.ACTIVITY_STOPPED));
                if (!resumed && !paused) continue;

                String className = event.getClassName();
                if (className == null || className.isEmpty()) className = "*";
                long timestamp = event.getTimeStamp();

                if (resumed) {
                    Map<String, Long> activities = resumedActivities.get(packageName);
                    if (activities == null) {
                        activities = new HashMap<>();
                        resumedActivities.put(packageName, activities);
                    }
                    activities.put(className, timestamp);
                } else {
                    Map<String, Long> activities = resumedActivities.get(packageName);
                    if (activities != null) {
                        if ("*".equals(className)
                                || type == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                            activities.clear();
                        } else {
                            activities.remove(className);
                        }
                        if (activities.isEmpty()) resumedActivities.remove(packageName);
                    }
                }
            }
            return true;
        } catch (Throwable error) {
            android.util.Log.w("NightScreenIdle",
                    "Unable to read foreground application events.", error);
            return false;
        }
    }

    private void updateForegroundState(long nowWall, boolean firstSnapshot) {
        String topPackage = null;
        long topTimestamp = Long.MIN_VALUE;
        for (Map.Entry<String, Map<String, Long>> packageEntry : resumedActivities.entrySet()) {
            for (Long timestamp : packageEntry.getValue().values()) {
                if (timestamp != null && timestamp > topTimestamp) {
                    topTimestamp = timestamp;
                    topPackage = packageEntry.getKey();
                }
            }
        }
        if (topPackage == null) {
            foregroundPackage = null;
            idleSinceElapsed = -1L;
            launchPending = false;
            return;
        }

        if (!topPackage.equals(foregroundPackage)) {
            foregroundPackage = topPackage;
            if (launcherPackage.equals(topPackage)) {
                long elapsedNow = SystemClock.elapsedRealtime();
                if (firstSnapshot) {
                    // Opt-in starts a fresh wait; time before enabling is not counted.
                    idleSinceElapsed = elapsedNow;
                } else {
                    long foregroundAge = Math.max(0L, nowWall - topTimestamp);
                    idleSinceElapsed = Math.max(0L, elapsedNow - foregroundAge);
                }
            } else {
                idleSinceElapsed = -1L;
                launchPending = false;
            }
        }
    }

    private void maybeLaunchFromIdle(SharedPreferences prefs) {
        if (!running || launchPending || idleSinceElapsed < 0L
                || !launcherPackage.equals(foregroundPackage)) return;
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (power == null || !power.isInteractive()) return;

        int seconds = Math.max(30, Math.min(600, prefs.getInt("idleWaitSeconds", 30)));
        long requiredMs = seconds * 1_000L;
        long elapsed = SystemClock.elapsedRealtime() - idleSinceElapsed;
        if (!shouldLaunch(prefs.getBoolean("autoStartOnIdle", false),
                true, power.isInteractive(), elapsed, requiredMs)) return;

        long nowElapsed = SystemClock.elapsedRealtime();
        if (nowElapsed - lastLaunchAttemptElapsed < LAUNCH_RETRY_COOLDOWN_MS) return;
        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(context);
        if (snapshot == null || !snapshot.autoStartOnIdle
                || !AutoStartConditions.canActivate(context, snapshot)) return;
        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) return;
        if (MainActivity.isMainActivitySessionAlive()) return;

        launchPending = true;
        lastLaunchAttemptElapsed = nowElapsed;
        idleSinceElapsed = nowElapsed;
        MonitorSettingsSnapshot.setSuppressNextDeviceLockScreenOff(context, true);
        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(MainActivity.EXTRA_OPENED_BY_IDLE, true);
        try {
            context.startActivity(launch);
        } catch (Throwable error) {
            launchPending = false;
            MonitorSettingsSnapshot.setSuppressNextDeviceLockScreenOff(context, false);
            android.util.Log.w("NightScreenIdle",
                    "Android blocked the foreground-app idle screensaver launch.", error);
        }
    }

    static boolean shouldLaunch(boolean enabled, boolean launcherForeground,
                                boolean screenInteractive, long idleElapsedMs,
                                long requiredIdleMs) {
        return enabled && launcherForeground && screenInteractive
                && requiredIdleMs > 0L && idleElapsedMs >= requiredIdleMs;
    }

    private boolean hasUsageAccess() {
        try {
            AppOpsManager manager = (AppOpsManager)
                    context.getSystemService(Context.APP_OPS_SERVICE);
            return manager != null && manager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), context.getPackageName())
                    == AppOpsManager.MODE_ALLOWED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private String resolveLauncherPackage() {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo result = context.getPackageManager()
                    .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
            return result == null || result.activityInfo == null
                    ? null : result.activityInfo.packageName;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
