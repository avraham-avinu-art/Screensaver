package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public final class ScreensaverNotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(
                "settings", Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean("autoStartOnDeviceLock", false)
                .putBoolean("autoStartOnLockOverlayPermissionPending", false)
                .apply();

        LockScreenMonitorService.sync(context);
        NightScreenApplication.syncChargerReceiver(context);
    }
}
