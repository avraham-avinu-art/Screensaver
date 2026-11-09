package com.avrahamart.nightscreen;

import android.app.Application;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

/**
 * Application helpers. Automatic-start receivers are intentionally not
 * registered here: LockScreenMonitorService owns the only dynamic receiver.
 */
public final class NightScreenApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Compatibility method retained for existing settings call sites.
     * Re-evaluates the single monitor service instead of creating another
     * process-specific charger receiver.
     */
    public static void syncChargerReceiver(Context context) {
        LockScreenMonitorService.sync(context);
    }

    static boolean hasNotificationAccess(Context context) {
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
