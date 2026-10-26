package com.avrahamart.nightscreen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;

public final class LockScreenMonitorService extends Service {
    private static final String CHANNEL_ID = "screensaver_lock_monitor_v1";
    private static final int NOTIFICATION_ID = 6721;
    private boolean receiverRegistered = false;
    private AutoStartOnPowerReceiver lockReceiver;

    /**
     * Starts monitoring only while the user has enabled automatic lock activation
     * and the background-activity-launch permission prerequisite is satisfied.
     */
    public static void sync(Context context) {
        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("autoStartOnDeviceLock", false);
        if (enabled && (Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(appContext))) {
            Intent intent = new Intent(appContext, LockScreenMonitorService.class);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent);
                } else {
                    appContext.startService(intent);
                }
            } catch (Throwable ignored) {
                // Android may block background service starts on some system versions.
            }
        } else {
            try {
                appContext.stopService(new Intent(appContext, LockScreenMonitorService.class));
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        lockReceiver = new AutoStartOnPowerReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(lockReceiver, filter, RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(lockReceiver, filter);
            }
            receiverRegistered = true;
        } catch (Throwable ignored) {
            receiverRegistered = false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        if (!prefs.getBoolean("autoStartOnDeviceLock", false)
                || (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(this))) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        Notification notification = buildQuietNotification();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Throwable ex) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "שירות רקע", NotificationManager.IMPORTANCE_MIN);
        channel.setDescription("שירות להפעלה אוטומטית בנעילת המכשיר");
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        if (Build.VERSION.SDK_INT >= 29) channel.setAllowBubbles(false);
        manager.createNotificationChannel(channel);
    }

    private Notification buildQuietNotification() {
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("שומר מסך")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_SECRET);
        builder.setPriority(Notification.PRIORITY_MIN)
                .setSound(null)
                .setVibrate(new long[0])
                .setDefaults(0);
        return builder.build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Deliberately keep monitoring when the user removes the app's task.
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if (receiverRegistered && lockReceiver != null) {
            try {
                unregisterReceiver(lockReceiver);
            } catch (Throwable ignored) {
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
