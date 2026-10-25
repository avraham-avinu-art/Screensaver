package com.avrahamart.nightscreen;

import android.app.ActivityOptions;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public final class AutoStartOnPowerReceiver extends BroadcastReceiver {
    public static final int POWER_NOTIFICATION_ID = 4307;
    private static final String CHANNEL_ID = "charger_autostart";
    private static final String CHANNEL_NAME = "הפעלה אוטומטית בטעינה";

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

        PendingIntent pendingIntent;
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                // The user explicitly enabled charger auto-start. Opt in to the
                // creator's BAL privilege only for this app's own MainActivity.
                ActivityOptions creatorOptions = ActivityOptions.makeBasic()
                        .setPendingIntentCreatorBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                pendingIntent = PendingIntent.getActivity(
                        context, POWER_NOTIFICATION_ID, launch, pendingFlags,
                        creatorOptions.toBundle());
            } else {
                pendingIntent = PendingIntent.getActivity(
                        context, POWER_NOTIFICATION_ID, launch, pendingFlags);
            }
        } catch (Throwable pendingIntentFailure) {
            // If PendingIntent creation fails, preserve a best-effort direct launch.
            try {
                context.startActivity(launch);
            } catch (Throwable ignored) {
            }
            return;
        }

        // Keep a tappable fallback visible. If the Activity actually opens, it
        // cancels this notification. If Android blocks the background launch,
        // the user can open the screensaver from the notification.
        postFallbackNotification(context, pendingIntent);

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                ActivityOptions senderOptions = ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                pendingIntent.send(
                        context, 0, null, null, null, null, senderOptions.toBundle());
            } else if (Build.VERSION.SDK_INT >= 33) {
                // This API exists from Android 13 (API 33); do not call it on API 32 or lower.
                ActivityOptions senderOptions = ActivityOptions.makeBasic();
                senderOptions.setPendingIntentBackgroundActivityLaunchAllowed(true);
                pendingIntent.send(
                        context, 0, null, null, null, null, senderOptions.toBundle());
            } else {
                // Older versions have no PendingIntent sender opt-in API.
                context.startActivity(launch);
            }
        } catch (Throwable launchFailure) {
            // Some Android/OEM builds reject background Activity starts.
            // Leave the fallback notification available for a user-initiated launch.
            try {
                context.startActivity(launch);
            } catch (Throwable ignored) {
                // Do not remove the fallback notification: a background launch may be blocked silently.
            }
        }
    }

    private void postFallbackNotification(Context context, PendingIntent pendingIntent) {
        try {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(
                        "אפשרות לפתוח את שומר המסך כאשר Android חוסם פתיחה אוטומטית ברקע.");
                manager.createNotificationChannel(channel);
            }

            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(context, CHANNEL_ID)
                    : new Notification.Builder(context);

            builder.setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                    .setContentTitle("שומר מסך")
                    .setContentText("לחץ כאן לפתיחת שומר המסך לאחר חיבור המטען")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            if (Build.VERSION.SDK_INT < 26) {
                builder.setPriority(Notification.PRIORITY_DEFAULT);
            }
            manager.notify(POWER_NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignored) {
            // Android 13+ can deny notifications until the user grants permission.
        } catch (Throwable ignored) {
        }
    }

    public static void cancelLaunchNotification(Context context) {
        try {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.cancel(POWER_NOTIFICATION_ID);
        } catch (Throwable ignored) {
        }
    }
}
