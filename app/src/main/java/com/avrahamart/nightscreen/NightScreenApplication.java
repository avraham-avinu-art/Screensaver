package com.avrahamart.nightscreen;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Process;

/**
 * Keeps a runtime charger receiver in the main app process while it is alive.
 * Background lock-monitor and notification-listener processes intentionally do
 * not register duplicate charger receivers.
 */
public final class NightScreenApplication extends Application {
    private AutoStartOnPowerReceiver chargerReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!isMainApplicationProcess()) return;

        chargerReceiver = new AutoStartOnPowerReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(chargerReceiver, filter, RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(chargerReceiver, filter);
            }
        } catch (Throwable ignored) {
            // The manifest receiver remains the cold-start fallback.
        }
    }

    private boolean isMainApplicationProcess() {
        if (Build.VERSION.SDK_INT >= 28) {
            return getPackageName().equals(Application.getProcessName());
        }
        ActivityManager manager =
                (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager != null) {
            try {
                java.util.List<ActivityManager.RunningAppProcessInfo> processes =
                        manager.getRunningAppProcesses();
                if (processes != null) {
                    for (ActivityManager.RunningAppProcessInfo process : processes) {
                        if (process.pid == Process.myPid()) {
                            return getPackageName().equals(process.processName);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }
}
