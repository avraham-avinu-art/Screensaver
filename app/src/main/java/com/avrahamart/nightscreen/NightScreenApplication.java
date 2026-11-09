package com.avrahamart.nightscreen;

import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;

public final class NightScreenApplication extends Application {
    private AutoStartOnPowerReceiver chargerReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        // Keep the user's automatic-charge switch; the preference selects which
        // activation route listens for that same setting.
        updateChargerReceiverRegistration();
    }

    /**
     * Re-evaluates the best already-running process for charger events after
     * the user changes charger, lock-monitor, or notification-listener settings.
     */
    public static void syncChargerReceiver(android.content.Context context) {
        android.content.Context app = context.getApplicationContext();
        if (app instanceof NightScreenApplication) {
            ((NightScreenApplication) app).updateChargerReceiverRegistration();
        }
    }

    private void updateChargerReceiverRegistration() {
        boolean enabled = shouldListenForChargerEvents();
        if (enabled && chargerReceiver == null) {
            chargerReceiver = new AutoStartOnPowerReceiver();
            IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
            try {
                if (Build.VERSION.SDK_INT >= 33) {
                    registerReceiver(chargerReceiver, filter, RECEIVER_NOT_EXPORTED);
                } else {
                    registerReceiver(chargerReceiver, filter);
                }
            } catch (Throwable ignored) {
                chargerReceiver = null;
            }
        } else if (!enabled && chargerReceiver != null) {
            try {
                unregisterReceiver(chargerReceiver);
            } catch (Throwable ignored) {
            }
            chargerReceiver = null;
        }
    }

    private boolean shouldListenForChargerEvents() {
        String processName = currentProcessName();
        String packageName = getPackageName();
        boolean notificationAccess = hasNotificationAccess(this);
        android.content.SharedPreferences prefs =
                getSharedPreferences("settings", MODE_PRIVATE);
        boolean chargingEnabled = prefs.getBoolean("autoStartOnCharge", false)
                && !prefs.getBoolean("useAndroidDreamForCharging", true);
        boolean lockMonitorEnabled = prefs.getBoolean("autoStartOnDeviceLock", false);

        // The system-bound notification-listener process remains alive while
        // notification access is granted, without pinning the screensaver UI.
        if ((packageName + ":notifications").equals(processName)) {
            return notificationAccess;
        }

        // LockScreenMonitorService registers the same receiver alongside its
        // SCREEN_OFF / USER_PRESENT receiver in its own persistent process.
        if ((packageName + ":lockmonitor").equals(processName)) {
            return false;
        }

        // Use the UI process only when no already-persistent process can listen.
        return packageName.equals(processName)
                && chargingEnabled && !notificationAccess && !lockMonitorEnabled;
    }

    static boolean hasNotificationAccess(android.content.Context context) {
        ComponentName component =
                new ComponentName(context, SystemMediaNotificationListener.class);
        try {
            if (Build.VERSION.SDK_INT >= 27) {
                NotificationManager manager =
                        (NotificationManager) context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
                if (manager != null
                        && manager.isNotificationListenerAccessGranted(component)) {
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

    private String currentProcessName() {
        if (Build.VERSION.SDK_INT >= 28) return Application.getProcessName();
        ActivityManager manager =
                (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager != null) {
            try {
                java.util.List<ActivityManager.RunningAppProcessInfo> processes =
                        manager.getRunningAppProcesses();
                if (processes != null) {
                    for (ActivityManager.RunningAppProcessInfo process : processes) {
                        if (process.pid == Process.myPid()) return process.processName;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return getPackageName();
    }
}
