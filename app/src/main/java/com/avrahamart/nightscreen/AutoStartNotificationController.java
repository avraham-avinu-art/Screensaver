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

/**
 * Builds the single persistent notification owned exclusively by
 * LockScreenMonitorService while that service is running in the foreground.
 * This class deliberately never posts a standalone notification.
 */
public final class AutoStartNotificationController {
    static final String CHANNEL_ID = "screensaver_lock_monitor_v6";
    static final int NOTIFICATION_ID = 6723;

    private AutoStartNotificationController() {
    }

    static Notification buildNotification(Context context) {
        Context app = context.getApplicationContext();

        NotificationManager manager =
                (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) createChannel(context, manager);

        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(app);
        boolean paused = snapshot != null && snapshot.autoStartPaused;
        String title = context.getString(paused
                ? R.string.auto_start_notification_text_005
                : R.string.auto_start_notification_text_001);
        String message = context.getString(paused
                ? R.string.auto_start_notification_text_006
                : R.string.auto_start_notification_text_002);
        String actionLabel = context.getString(paused
                ? R.string.auto_start_notification_text_008
                : R.string.auto_start_notification_text_007);

        Intent toggleIntent = new Intent(app, AutoStartPauseReceiver.class)
                .setAction(paused
                        ? AutoStartPauseReceiver.ACTION_RESUME
                        : AutoStartPauseReceiver.ACTION_PAUSE)
                .setPackage(app.getPackageName());
        int requestCode = paused ? NOTIFICATION_ID + 2 : NOTIFICATION_ID + 1;
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent togglePendingIntent = PendingIntent.getBroadcast(
                app, requestCode, toggleIntent, pendingFlags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(app, CHANNEL_ID)
                : new Notification.Builder(app);
        // The service still requires a valid small icon, but its fully transparent
        // artwork keeps the status-bar slot visually empty. The large app icon
        // remains visible inside the notification shade.
        builder.setSmallIcon(R.drawable.ic_notification_transparent)
                .setLargeIcon(createApplicationIcon(app))
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                // Tapping the notification body does not open Settings or cancel the service.
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setPriority(Notification.PRIORITY_MIN)
                .setSound(null)
                .setVibrate(new long[0])
                .setDefaults(0)
                .addAction(paused ? android.R.drawable.ic_media_play
                                : android.R.drawable.ic_media_pause,
                        actionLabel, togglePendingIntent);

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
        if (manager != null) createChannel(context, manager);
    }

    private static void createChannel(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, context.getString(R.string.auto_start_notification_text_003), NotificationManager.IMPORTANCE_MIN);
        channel.setDescription(context.getString(R.string.auto_start_notification_text_004));
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }
}
