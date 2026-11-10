package com.avrahamart.nightscreen;

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

        if (!disableLockStart && !disableChargeStart && !disableBootStart
                && !disableWakeStart && !disablePlayer) return "";

        SharedPreferences.Editor editor = prefs.edit();
        if (disableLockStart) editor.putBoolean("autoStartOnDeviceLock", false)
                .putBoolean("autoStartOnLockOverlayPermissionPending", false);
        if (disableChargeStart) editor.putBoolean("autoStartOnCharge", false)
                .putBoolean("autoStartOverlayPermissionPending", false);
        if (disableBootStart) editor.putBoolean("autoStartOnBoot", false);
        if (disableWakeStart) editor.putBoolean("autoStartOnWake", false);
        if (disablePlayer) editor.putBoolean("showPlayer", false)
                .putBoolean("showPlayerPermissionPending", false);
        editor.apply();

        if (disableLockStart || disableWakeStart) {
            LockScreenMonitorService.sync(context);
        }
        if (disableLockStart || disableChargeStart || disableWakeStart) {
            NightScreenApplication.syncChargerReceiver(context);
        }

        ArrayList<String> messages = new ArrayList<>();
        if (disableLockStart) {
            messages.add("ההפעלה האוטומטית בנעילה כובתה מפני שהרשאת ההצגה מעל אפליקציות אחרות בוטלה.");
        }
        if (disableChargeStart) {
            messages.add("ההפעלה האוטומטית בטעינה כובתה מפני שהרשאת ההצגה מעל אפליקציות אחרות בוטלה.");
        }
        if (disableBootStart) {
            messages.add("ההפעלה האוטומטית בהפעלת המכשיר כובתה מפני שהרשאת ההצגה מעל אפליקציות אחרות בוטלה.");
        }
        if (disableWakeStart) {
            messages.add("ההפעלה האוטומטית בהתעוררות כובתה מפני שהרשאת ההצגה מעל אפליקציות אחרות בוטלה.");
        }
        if (disablePlayer) {
            messages.add("הצגת הנגן כובתה מפני שהרשאת הגישה להתראות בוטלה.");
        }
        return android.text.TextUtils.join("\n", messages);
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
