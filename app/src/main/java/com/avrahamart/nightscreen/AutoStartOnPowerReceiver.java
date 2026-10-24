package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public final class AutoStartOnPowerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null
                || !Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("autoStartOnCharge", false)) {
            return;
        }

        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(MainActivity.EXTRA_OPENED_BY_CHARGER, true);

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;

        try {
            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= 34) {
                // Android 14+ requires both the creator and sender to opt in to
                // a user-authorized background activity launch from this receiver.
                ActivityOptions creatorOptions = ActivityOptions.makeBasic()
                        .setPendingIntentCreatorBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                pendingIntent = PendingIntent.getActivity(
                        context, 4307, launch, pendingFlags, creatorOptions.toBundle());

                int senderMode = Build.VERSION.SDK_INT >= 36
                        ? ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                        : ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED;
                ActivityOptions senderOptions = ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(senderMode);
                pendingIntent.send(context, 0, null, null, null, null, senderOptions.toBundle());
            } else if (Build.VERSION.SDK_INT >= 33) {
                pendingIntent = PendingIntent.getActivity(
                        context, 4307, launch, pendingFlags);
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setPendingIntentBackgroundActivityLaunchAllowed(true);
                pendingIntent.send(context, 0, null, null, null, null, options.toBundle());
            } else {
                // Legacy Android versions have no sender opt-in API.
                context.startActivity(launch);
            }
        } catch (Throwable launchFailure) {
            // Keep a compatibility attempt for devices that reject the opt-in path.
            try {
                context.startActivity(launch);
            } catch (Throwable ignored) {
                // Android/OEM background-launch policy may still block the Activity.
            }
        }
    }
}
