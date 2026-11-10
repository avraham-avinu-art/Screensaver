package com.avrahamart.nightscreen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/**
 * Application-level notification for the user's automatic-start choices.
 *
 * This is deliberately a normal NotificationManager notification, not the
 * foreground-service notification and not tied to any Activity lifecycle.
 */
public final class AutoStartNotificationController {
    private static final String CHANNEL_ID = "screensaver_auto_start_status";
    private static final int NOTIFICATION_ID = 6723;

    private AutoStartNotificationController() {
    }

    public static void sync(Context context) {
        Context app = context.getApplicationContext();
        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(app);
        if (snapshot == null) return;
        boolean enabled = snapshot.autoStartOnDeviceLock
                || snapshot.autoStartOnBoot
                || snapshot.autoStartOnWake
                || snapshot.autoStartOnCharge;

        NotificationManager manager =
                (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (!enabled) {
            manager.cancel(NOTIFICATION_ID);
            return;
        }

        if (Build.VERSION.SDK_INT >= 33
                && app.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        createChannel(manager);
        Intent settings = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        // Keep the package-scoped destination on Android 11+ as well. This is the
        // same explicit destination used by the tested Key Mapper notification flow.
        settings.setData(Uri.parse("package:" + app.getPackageName()));
        settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent contentIntent = PendingIntent.getActivity(
                app, NOTIFICATION_ID, settings, pendingFlags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(app, CHANNEL_ID)
                : new Notification.Builder(app);
        builder.setSmallIcon(R.drawable.ic_qs_screensaver)
                .setContentTitle("שומר מסך")
                .setContentText("ההפעלה האוטומטית פעילה")
                .setContentIntent(contentIntent)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setPriority(Notification.PRIORITY_MIN)
                .setSound(null)
                .setVibrate(new long[0])
                .setDefaults(0);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        manager.notify(NOTIFICATION_ID, notification);
    }

    private static void createChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "הפעלה אוטומטית", NotificationManager.IMPORTANCE_MIN);
        channel.setDescription("התראה מתמשכת עבור הגדרות ההפעלה האוטומטית של שומר מסך");
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }
}
