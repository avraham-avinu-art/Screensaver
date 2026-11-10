package com.avrahamart.nightscreen;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

/** Helpers for checking system-managed notification access. */
public final class NightScreenApplication extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
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
