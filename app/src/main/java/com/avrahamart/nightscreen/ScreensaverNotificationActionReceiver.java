package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Toggles the screensaver from the persistent foreground-service notification. */
public final class ScreensaverNotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (MainActivity.isMainActivitySessionAlive()) {
            launch.setAction(MainActivity.ACTION_FINISH_FROM_NOTIFICATION);
        }
        try {
            context.startActivity(launch);
        } catch (Throwable ignored) {
        }
    }
}
