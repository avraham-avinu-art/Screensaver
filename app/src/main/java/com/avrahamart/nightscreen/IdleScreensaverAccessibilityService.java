package com.avrahamart.nightscreen;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.Locale;

/**
 * Detects idle time while the launcher's home screen is in the foreground.
 * The service does not retrieve window contents, inspect text, or perform
 * gestures; it observes only touch-interaction and window-state event types.
 */
public final class IdleScreensaverAccessibilityService extends AccessibilityService {
    private static final String TAG = "NightScreenIdle";
    private static final long MIN_IDLE_MS = 30_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String homePackage;
    private String foregroundPackage;
    private long lastInteractionElapsed;
    private boolean launchPending;

    private final Runnable idleCheck = new Runnable() {
        @Override
        public void run() {
            if (launchPending || !getSharedPreferences("settings", MODE_PRIVATE)
                    .getBoolean("autoStartOnIdle", false)) return;
            if (!isHomeInForeground() || !isScreenInteractive()) return;

            int seconds = Math.max(30, Math.min(600,
                    getSharedPreferences("settings", MODE_PRIVATE)
                            .getInt("idleWaitSeconds", 30)));
            long requiredMs = Math.max(MIN_IDLE_MS, seconds * 1000L);
            long elapsed = android.os.SystemClock.elapsedRealtime()
                    - lastInteractionElapsed;
            if (elapsed < requiredMs) {
                handler.postDelayed(this, requiredMs - elapsed);
                return;
            }
            launchFromIdle();
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    | AccessibilityEvent.TYPE_TOUCH_INTERACTION_START;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.notificationTimeout = 80L;
            info.flags &= ~AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            setServiceInfo(info);
        }
        homePackage = findHomePackage();
        lastInteractionElapsed = android.os.SystemClock.elapsedRealtime();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START) {
            lastInteractionElapsed = android.os.SystemClock.elapsedRealtime();
            launchPending = false;
            handler.removeCallbacks(idleCheck);
            if (isHomeInForeground()) scheduleIdleCheck();
            return;
        }

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageName = event.getPackageName();
            foregroundPackage = packageName == null ? null : packageName.toString();
            launchPending = false;
            handler.removeCallbacks(idleCheck);
            if (isHomeInForeground()) {
                // Returning to the launcher begins a fresh idle period.
                lastInteractionElapsed = android.os.SystemClock.elapsedRealtime();
                scheduleIdleCheck();
            }
        }
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacks(idleCheck);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(idleCheck);
        super.onDestroy();
    }

    private void scheduleIdleCheck() {
        if (!getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("autoStartOnIdle", false)) return;
        int seconds = Math.max(30, Math.min(600,
                getSharedPreferences("settings", MODE_PRIVATE)
                        .getInt("idleWaitSeconds", 30)));
        long requiredMs = Math.max(MIN_IDLE_MS, seconds * 1000L);
        long elapsed = android.os.SystemClock.elapsedRealtime() - lastInteractionElapsed;
        handler.postDelayed(idleCheck, Math.max(1L, requiredMs - elapsed));
    }

    private boolean isHomeInForeground() {
        if (homePackage == null) homePackage = findHomePackage();
        return homePackage != null && homePackage.equals(foregroundPackage);
    }

    private String findHomePackage() {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            android.content.pm.ResolveInfo resolved = getPackageManager()
                    .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolved != null && resolved.activityInfo != null) {
                return resolved.activityInfo.packageName;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private boolean isScreenInteractive() {
        PowerManager power = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return power != null && power.isInteractive();
    }

    private void launchFromIdle() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("settings", MODE_PRIVATE);
        if (!prefs.getBoolean("autoStartOnIdle", false)
                || !isHomeInForeground() || !isScreenInteractive()) return;
        if (android.os.Build.VERSION.SDK_INT >= 29
                && !Settings.canDrawOverlays(this)) return;

        int seconds = Math.max(30, Math.min(600, prefs.getInt("idleWaitSeconds", 30)));
        long requiredMs = seconds * 1000L;
        if (android.os.SystemClock.elapsedRealtime() - lastInteractionElapsed < requiredMs) {
            scheduleIdleCheck();
            return;
        }

        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        if (snapshot == null || !AutoStartConditions.canActivate(this, snapshot)) {
            lastInteractionElapsed = android.os.SystemClock.elapsedRealtime();
            return;
        }
        if (MainActivity.isMainActivitySessionAlive()) return;

        launchPending = true;
        lastInteractionElapsed = android.os.SystemClock.elapsedRealtime();
        MonitorSettingsSnapshot.setSuppressNextDeviceLockScreenOff(this, true);

        Intent launch = new Intent(this, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(MainActivity.EXTRA_OPENED_BY_IDLE, true);
        try {
            startActivity(launch);
        } catch (Throwable error) {
            launchPending = false;
            MonitorSettingsSnapshot.setSuppressNextDeviceLockScreenOff(this, false);
            Log.w(TAG, "Android blocked the idle screensaver launch.", error);
            scheduleIdleCheck();
        }
    }

    public static boolean isEnabled(Context context) {
        try {
            if (Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0) != 1) return false;
            String enabled = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (TextUtils.isEmpty(enabled)) return false;
            ComponentName expected =
                    new ComponentName(context, IdleScreensaverAccessibilityService.class);
            String flat = expected.flattenToString();
            for (String entry : enabled.split(":")) {
                String candidate = entry.trim();
                if (flat.equalsIgnoreCase(candidate)
                        || expected.equals(ComponentName.unflattenFromString(candidate))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
