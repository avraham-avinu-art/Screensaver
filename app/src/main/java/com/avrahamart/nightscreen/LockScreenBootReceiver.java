package com.avrahamart.nightscreen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/** Handles only system boot and app replacement; never treats either as a lock event. */
public final class LockScreenBootReceiver extends BroadcastReceiver {
    private static final String TAG = "NightScreenAutoStart";
    private static final String BOOT_NOTIFICATION_CHANNEL_ID =
            "screensaver_boot_start_status_v1";
    private static final int BOOT_NOTIFICATION_ID = 6723;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;

        SharedPreferences prefs =
                context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Each completed reboot begins a fresh lock cycle.
            prefs.edit().putBoolean("deviceLockStartConsumed", false).apply();
        }

        // Starts the monitor only if lock, wake, or app-managed charging is enabled.
        // Boot itself is exclusively handled below.
        LockScreenMonitorService.sync(context);

        boolean bootEnabled = AutoStartPolicy.isEnabledForAction(
                action,
                false,
                prefs.getBoolean("autoStartOnBoot", false),
                false,
                false,
                false);
        if (!bootEnabled) return;

        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Boot auto-start is enabled, but SYSTEM_ALERT_WINDOW was not granted.");
            return;
        }

        // The PendingIntent includes the explicit BAL sender/creator opt-ins used
        // by Android 14+ and Android 15+ for a launch requested from a receiver.
        boolean launched = AutoStartActivityLauncher.launch(
                context, AutoStartPolicy.Trigger.DEVICE_BOOT);
        if (!launched) {
            Log.e(TAG, "Unable to start the screensaver from BOOT_COMPLETED.");
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            showBootStartedNotification(context);
        }
    }

    /**
     * A low-importance notification gives a status-bar icon and a drawer entry
     * without interrupting the user with sound, vibration, or a heads-up banner.
     */
    private void showBootStartedNotification(Context context) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Boot notification skipped: POST_NOTIFICATIONS is not granted.");
            return;
        }

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    BOOT_NOTIFICATION_CHANNEL_ID,
                    "הפעלה אוטומטית לאחר אתחול",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("הודעה שקטה לאחר הפעלה אוטומטית של שומר מסך.");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            channel.setSound(null, null);
            channel.enableVibration(false);
            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, BOOT_NOTIFICATION_ID, openIntent, pendingFlags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, BOOT_NOTIFICATION_CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification_transparent)
                .setContentTitle("שומר מסך הופעל")
                .setContentText("ההפעלה האוטומטית לאחר אתחול המכשיר הושלמה.")
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setSound(null)
                .setVibrate(new long[0])
                .setDefaults(0);
        if (Build.VERSION.SDK_INT < 26) {
            builder.setPriority(Notification.PRIORITY_LOW);
        }
        manager.notify(BOOT_NOTIFICATION_ID, builder.build());
    }
}
