package com.avrahamart.nightscreen;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

/**
 * Registers power and screen-lock monitoring while the app process is alive.
 * ACTION_SCREEN_OFF and ACTION_USER_PRESENT must be registered at runtime.
 */
public final class NightScreenApplication extends Application {
    private AutoStartOnPowerReceiver chargerReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        chargerReceiver = new AutoStartOnPowerReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(chargerReceiver, filter, RECEIVER_EXPORTED);
            } else {
                registerReceiver(chargerReceiver, filter);
            }
        } catch (Throwable ignored) {
            // The manifest receiver remains available as a fallback for charger events.
        }
    }
}
