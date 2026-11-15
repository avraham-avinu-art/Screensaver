package com.avrahamart.nightscreen;

import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

import java.util.ArrayList;

/**
 * Keeps persisted feature switches consistent with the permissions they require.
 * Called whenever the app returns from Android settings, not only during the
 * first permission-request flow.
 */
public final class PermissionDependencySync {
    private PermissionDependencySync() {
    }

    public static String disableFeaturesWithoutPermissions(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean overlayRequired = Build.VERSION.SDK_INT >= 29
                && !Settings.canDrawOverlays(context);
        boolean notificationAccess = hasNotificationAccess(context);
        boolean usageAccess = hasUsageAccess(context);

        boolean disableLockStart =
                prefs.getBoolean("autoStartOnDeviceLock", false) && overlayRequired;
        boolean disableChargeStart =
                prefs.getBoolean("autoStartOnCharge", false)
                        && !prefs.getBoolean("useAndroidDreamForCharging", false)
                        && overlayRequired;
        boolean disableBootStart =
                prefs.getBoolean("autoStartOnBoot", false) && overlayRequired;
        boolean disableWakeStart =
                prefs.getBoolean("autoStartOnWake", false) && overlayRequired;
        boolean disablePlayer =
                prefs.getBoolean("showPlayer", false) && !notificationAccess;
        boolean disableUseProtection =
                prefs.getBoolean("preventAutoStartDuringUse", false) && !usageAccess;
        boolean disableExcludedApps =
                prefs.getBoolean("pauseWhenExcludedAppActive", false) && !usageAccess;

        if (!disableLockStart && !disableChargeStart && !disableBootStart
                && !disableWakeStart && !disablePlayer
                && !disableUseProtection && !disableExcludedApps) return "";

        SharedPreferences.Editor editor = prefs.edit();
        if (disableLockStart) editor.putBoolean("autoStartOnDeviceLock", false)
                .putBoolean("autoStartOnLockOverlayPermissionPending", false);
        if (disableChargeStart) editor.putBoolean("autoStartOnCharge", false)
                .putBoolean("autoStartOverlayPermissionPending", false);
        if (disableBootStart) editor.putBoolean("autoStartOnBoot", false);
        if (disableWakeStart) editor.putBoolean("autoStartOnWake", false);
        if (disablePlayer) editor.putBoolean("showPlayer", false)
                .putBoolean("showPlayerPermissionPending", false);
        if (disableUseProtection) editor.putBoolean("preventAutoStartDuringUse", false);
        if (disableExcludedApps) editor.putBoolean("pauseWhenExcludedAppActive", false);
        editor.apply();
        if (disableLockStart) {
            MonitorSettingsSnapshot.resetDeviceLockActivation(context);
        }

        if (disableLockStart || disableWakeStart) {
            LockScreenMonitorService.sync(context);
        }
        if (disableLockStart || disableChargeStart || disableWakeStart) {
            NightScreenApplication.syncChargerReceiver(context);
        }

        ArrayList<String> messages = new ArrayList<>();
        if (disableLockStart) {
            messages.add(context.getString(R.string.permission_sync_text_001));
        }
        if (disableChargeStart) {
            messages.add(context.getString(R.string.permission_sync_text_002));
        }
        if (disableBootStart) {
            messages.add(context.getString(R.string.permission_sync_text_003));
        }
        if (disableWakeStart) {
            messages.add(context.getString(R.string.permission_sync_text_004));
        }
        if (disablePlayer) {
            messages.add(context.getString(R.string.permission_sync_text_005));
        }
        if (disableUseProtection || disableExcludedApps) {
            messages.add(context.getString(R.string.permission_sync_text_006));
        }
        return android.text.TextUtils.join("\n", messages);
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

    private static boolean hasNotificationAccess(Context context) {
        ComponentName component =
                new ComponentName(context, SystemMediaNotificationListener.class);
        try {
            if (Build.VERSION.SDK_INT >= 27) {
                NotificationManager manager = (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null && manager.isNotificationListenerAccessGranted(component)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            String enabled = Settings.Secure.getString(
                    context.getContentResolver(), "enabled_notification_listeners");
            return enabled != null && enabled.contains(component.flattenToString());
        } catch (Throwable ignored) {
            return false;
        }
    }
}
