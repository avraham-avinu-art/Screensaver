package com.avrahamart.nightscreen;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/** Launches MainActivity with one and only one automatic-start source marker. */
final class AutoStartActivityLauncher {
    private static final String TAG = "NightScreenAutoStart";
    private static final int REQUEST_LOCK = 7411;
    private static final int REQUEST_BOOT = 7412;
    private static final int REQUEST_WAKE = 7413;
    private static final int REQUEST_CHARGE = 7414;

    private AutoStartActivityLauncher() {}

    static boolean launch(Context context, AutoStartPolicy.Trigger trigger) {
        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final int requestCode;
        switch (trigger) {
            case DEVICE_LOCK:
                requestCode = REQUEST_LOCK;
                launchIntent.putExtra(MainActivity.EXTRA_OPENED_BY_DEVICE_LOCK, true);
                break;
            case DEVICE_BOOT:
                requestCode = REQUEST_BOOT;
                launchIntent.putExtra(MainActivity.EXTRA_OPENED_BY_BOOT, true);
                break;
            case DEVICE_WAKE:
                requestCode = REQUEST_WAKE;
                launchIntent.putExtra(MainActivity.EXTRA_OPENED_BY_WAKE, true);
                break;
            case CHARGER:
                requestCode = REQUEST_CHARGE;
                launchIntent.putExtra(MainActivity.EXTRA_OPENED_BY_CHARGER, true);
                break;
            default:
                return false;
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;

        Bundle creatorOptions = null;
        if (Build.VERSION.SDK_INT >= 34) {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
            creatorOptions = options.toBundle();
        }

        try {
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, requestCode, launchIntent, flags, creatorOptions);
            if (Build.VERSION.SDK_INT >= 34) {
                ActivityOptions senderOptions = ActivityOptions.makeBasic();
                senderOptions.setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                pendingIntent.send(senderOptions.toBundle());
            } else {
                pendingIntent.send();
            }
            Log.i(TAG, "Screensaver started by " + trigger);
            return true;
        } catch (PendingIntent.CanceledException | RuntimeException error) {
            Log.e(TAG, "Background launch failed for " + trigger, error);
            return false;
        }
    }
}
