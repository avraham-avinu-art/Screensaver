package com.avrahamart.nightscreen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;

/**
 * Builds the single persistent notification owned exclusively by
 * LockScreenMonitorService while that service is running in the foreground.
 * This class deliberately never posts a standalone notification.
 */
public final class AutoStartNotificationController {
    static final String CHANNEL_ID = "screensaver_lock_monitor_v4";
    static final int NOTIFICATION_ID = 6723;

    private AutoStartNotificationController() {
    }

    static Notification buildNotification(Context context) {
        Context app = context.getApplicationContext();

        NotificationManager manager =
                (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) createChannel(manager);

        Intent cancelService = new Intent(app, CancelAutoStartServiceReceiver.class);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent cancelIntent = PendingIntent.getBroadcast(
                app, NOTIFICATION_ID, cancelService, pendingFlags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(app, CHANNEL_ID)
                : new Notification.Builder(app);
        builder.setSmallIcon(R.drawable.ic_qs_screensaver)
                .setLargeIcon(createApplicationIcon(app))
                .setContentTitle("שירות ההפעלה האוטומטית פועל.")
                .setContentText(null)
                .setStyle(new Notification.BigTextStyle()
                        .bigText("לחץ לביטול השירות"))
                .setContentIntent(cancelIntent)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPriority(Notification.PRIORITY_MIN)
                .setSound(null)
                .setVibrate(new long[0])
                .setDefaults(0);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        return notification;
    }

    private static Bitmap createApplicationIcon(Context context) {
        try {
            Drawable drawable = context.getDrawable(R.drawable.ic_launcher);
            if (drawable == null) return null;
            int size = Math.max(1, Math.round(48f
                    * context.getResources().getDisplayMetrics().density));
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, size, size);
            drawable.draw(canvas);
            return bitmap;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static void createChannel(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) createChannel(manager);
    }

    private static void createChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "הפעלה אוטומטית של שומר מסך", NotificationManager.IMPORTANCE_MIN);
        channel.setDescription("התראה מתמשכת לשירות ההפעלה האוטומטית");
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }
}
