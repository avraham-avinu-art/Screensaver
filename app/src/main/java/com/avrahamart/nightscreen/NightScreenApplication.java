package com.avrahamart.nightscreen;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

/**
 * Keeps the existing charger-triggered launch receiver available while the
 * application process is alive. Lock-state monitoring belongs to the foreground service.
 */
public final class NightScreenApplication extends Application {
    private AutoStartOnPowerReceiver chargerReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        chargerReceiver = new AutoStartOnPowerReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
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
