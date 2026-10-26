package com.avrahamart.nightscreen;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;

public final class LockScreenMonitorService extends Service {
    // v2 enforces the minimum supported FGS notification importance; Android will
    // retain the old channel's importance otherwise even after its settings change.
    private static final String CHANNEL_ID = "screensaver_lock_monitor_v2";
    private static final int NOTIFICATION_ID = 6721;
    private static final int RESTART_PENDING_INTENT_ID = 6722;
    private static final long RESTART_DELAY_MS = 2_000L;
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

        cancelScheduledRestart();
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
                CHANNEL_ID, "שירות רקע", NotificationManager.IMPORTANCE_LOW);
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
        builder.setPriority(Notification.PRIORITY_LOW)
                .setSound(null)
                .setVibrate(new long[0])
                .setDefaults(0);
        return builder.build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // The foreground service normally survives task removal. Schedule one
        // recovery start as a fallback for devices that terminate it with the task.
        scheduleRestartIfEnabled();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        // Do not schedule a restart when the user explicitly turns the feature off.
        scheduleRestartIfEnabled();
        if (receiverRegistered && lockReceiver != null) {
            try {
                unregisterReceiver(lockReceiver);
            } catch (Throwable ignored) {
            }
        }
        super.onDestroy();
    }

    private void scheduleRestartIfEnabled() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        if (!prefs.getBoolean("autoStartOnDeviceLock", false)) return;
        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(this)) return;

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        PendingIntent restartIntent = restartPendingIntent();
        if (restartIntent == null) return;
        try {
            long triggerAt = SystemClock.elapsedRealtime() + RESTART_DELAY_MS;
            if (Build.VERSION.SDK_INT >= 23) {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, restartIntent);
            } else {
                alarmManager.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, restartIntent);
            }
        } catch (Throwable ignored) {
        }
    }

    private void cancelScheduledRestart() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent restartIntent = restartPendingIntent();
        if (alarmManager != null && restartIntent != null) {
            try {
                alarmManager.cancel(restartIntent);
            } catch (Throwable ignored) {
            }
        }
    }

    private PendingIntent restartPendingIntent() {
        Intent intent = new Intent(this, LockScreenServiceRestartReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        try {
            return PendingIntent.getBroadcast(this, RESTART_PENDING_INTENT_ID, intent, flags);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
