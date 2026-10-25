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
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

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

        PendingIntent fallbackIntent = createFallbackPendingIntent(context, launch);
        if (fallbackIntent == null) return;

        // Android 10+ restricts starting activities from the background. An
        // explicit user-granted SYSTEM_ALERT_WINDOW permission is a documented
        // exception. Ask for it while the auto-start toggle is enabled, then
        // attempt a direct launch when it is granted.
        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) {
            postFallbackNotification(context, fallbackIntent);
            return;
        }

        try {
            context.startActivity(launch);
        } catch (Throwable launchFailure) {
            // Keep notification as a fallback only when the direct launch fails.
            postFallbackNotification(context, fallbackIntent);
        }
    }

    private PendingIntent createFallbackPendingIntent(Context context, Intent launch) {
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                // Android 14+ requires an explicit creator-side BAL opt-in for
                // PendingIntents targeting an Activity on Android 15+.
                ActivityOptions creatorOptions = ActivityOptions.makeBasic()
                        .setPendingIntentCreatorBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                return PendingIntent.getActivity(
                        context, POWER_NOTIFICATION_ID, launch, pendingFlags,
                        creatorOptions.toBundle());
            }
            return PendingIntent.getActivity(context, POWER_NOTIFICATION_ID, launch, pendingFlags);
        } catch (Throwable ignored) {
            return null;
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
                        "אפשרות לפתוח את שומר המסך כאשר פתיחה אוטומטית נחסמת.");
                manager.createNotificationChannel(channel);
            }

            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(context, CHANNEL_ID)
                    : new Notification.Builder(context);

            builder.setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                    .setContentTitle("שומר מסך")
                    .setContentText("הפתיחה האוטומטית נחסמה. לחץ כאן לפתיחת שומר המסך.")
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
