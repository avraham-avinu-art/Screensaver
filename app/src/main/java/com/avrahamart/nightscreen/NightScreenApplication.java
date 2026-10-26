package com.avrahamart.nightscreen;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

/**
 * Listens for charger events whenever the app process is alive.
 *
 * Manifest-declared receivers are restricted for most implicit broadcasts
 * on Android 8 and later, so charger events must also be registered at runtime.
 */
public final class NightScreenApplication extends Application {
    private AutoStartOnPowerReceiver chargerReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        chargerReceiver = new AutoStartOnPowerReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);

        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(chargerReceiver, filter, RECEIVER_EXPORTED);
            } else {
                registerReceiver(chargerReceiver, filter);
            }
            Log.i("NightScreenAutoStart", "Runtime charger receiver registered.");
        } catch (Throwable error) {
            Log.e("NightScreenAutoStart", "Unable to register runtime charger receiver.", error);
        }
    }
}
