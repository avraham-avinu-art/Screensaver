package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

/** Restores opt-in lock monitoring after a normal device reboot or app replacement. */
public final class LockScreenBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("autoStartOnDeviceLock", false)) return;
        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) return;

        LockScreenMonitorService.sync(context);
    }
}
