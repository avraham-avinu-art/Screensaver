package com.avrahamart.nightscreen;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

/**
 * Registers charger monitoring while the app process is alive.
 * The manifest receiver is retained for cases where Android starts the app
 * process to deliver a charger event.
 */
public final class NightScreenApplication extends Application {
    private AutoStartOnPowerReceiver chargerReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        chargerReceiver = new AutoStartOnPowerReceiver();

        IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(chargerReceiver, filter, RECEIVER_EXPORTED);
            } else {
                registerReceiver(chargerReceiver, filter);
            }
        } catch (Throwable ignored) {
            // The manifest receiver remains available as a fallback.
        }
    }
}
