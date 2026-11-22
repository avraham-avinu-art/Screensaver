package com.avrahamart.nightscreen;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;

/**
 * Starts the screensaver after Android reports no user interaction for the selected period.
 * Uses UsageStats events and Usage Access; it does not require an AccessibilityService.
 */
final class IdleScreensaverMonitor {
    private static final long POLL_INTERVAL_MS = 2_000L;
    private static final long QUERY_OVERLAP_MS = 1_000L;
    private static final long LAUNCH_RETRY_COOLDOWN_MS = 10_000L;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = this::poll;

    private UsageStatsManager usageStatsManager;
    private long lastUsageQueryWallTime;
    private long lastUserActivityWallTime;
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
        if (usageStatsManager == null) return;

        running = true;
        launchPending = false;
        long nowWall = System.currentTimeMillis();
        long nowElapsed = SystemClock.elapsedRealtime();

        // Enabling or restarting starts a full new wait. Earlier inactivity is not counted.
        lastUsageQueryWallTime = nowWall;
        lastUserActivityWallTime = nowWall;
        idleSinceElapsed = nowElapsed;

        handler.removeCallbacks(pollRunnable);
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    void stop() {
        running = false;
        handler.removeCallbacks(pollRunnable);
        idleSinceElapsed = -1L;
        launchPending = false;
        usageStatsManager = null;
    }

    private void poll() {
        if (!running) return;
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("autoStartOnIdle", false) || !hasUsageAccess()) {
            stop();
            return;
        }

        long nowWall = System.currentTimeMillis();
        long begin = Math.max(0L, lastUsageQueryWallTime - QUERY_OVERLAP_MS);
        if (nowWall > begin && readUsageEvents(begin, nowWall)) {
            lastUsageQueryWallTime = nowWall;
            maybeLaunchFromIdle(prefs);
        }
        if (running) handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private boolean readUsageEvents(long begin, long end) {
        try {
            UsageEvents events = usageStatsManager.queryEvents(begin, end);
            if (events == null) return false;

            UsageEvents.Event event = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                int type = event.getEventType();

                boolean activityResumed = type == UsageEvents.Event.MOVE_TO_FOREGROUND
                        || (Build.VERSION.SDK_INT >= 29
                        && type == UsageEvents.Event.ACTIVITY_RESUMED);
                boolean userInteraction = type == UsageEvents.Event.USER_INTERACTION;
                boolean screenBecameInteractive = Build.VERSION.SDK_INT >= 28
                        && type == UsageEvents.Event.SCREEN_INTERACTIVE;

                if (activityResumed || userInteraction || screenBecameInteractive) {
                    recordUserActivity(event.getTimeStamp(), end);
                }
            }
            return true;
        } catch (Throwable error) {
            android.util.Log.w("NightScreenIdle",
                    "Unable to read user interaction events.", error);
            return false;
        }
    }

    private void recordUserActivity(long eventWallTime, long nowWallTime) {
        if (eventWallTime <= lastUserActivityWallTime) return;

        lastUserActivityWallTime = eventWallTime;
        long ageMs = Math.max(0L, nowWallTime - eventWallTime);
        idleSinceElapsed = Math.max(0L, SystemClock.elapsedRealtime() - ageMs);
        launchPending = false;
    }

    private void maybeLaunchFromIdle(SharedPreferences prefs) {
        if (!running || launchPending || idleSinceElapsed < 0L) return;
        PowerManager power =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (power == null || !power.isInteractive()) return;

        int seconds = Math.max(30, Math.min(600, prefs.getInt("idleWaitSeconds", 30)));
        long requiredMs = seconds * 1_000L;
        long elapsed = SystemClock.elapsedRealtime() - idleSinceElapsed;
        if (!shouldLaunch(prefs.getBoolean("autoStartOnIdle", false),
                power.isInteractive(), elapsed, requiredMs)) return;

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
        lastUserActivityWallTime = System.currentTimeMillis();
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
                    "Android blocked the user-idle screensaver launch.", error);
        }
    }

    static boolean shouldLaunch(boolean enabled, boolean screenInteractive,
                                long idleElapsedMs, long requiredIdleMs) {
        return enabled && screenInteractive
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
}
