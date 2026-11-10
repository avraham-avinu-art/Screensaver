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
    // Internal foreground-service channel; the user-facing persistent notification
    // is posted separately by AutoStartNotificationController.
    private static final String CHANNEL_ID = "screensaver_lock_monitor_v4";
    private static final int NOTIFICATION_ID = 6721;
    private static final int RESTART_PENDING_INTENT_ID = 6722;
    private static final long RESTART_DELAY_MS = 2_000L;
    static final String EXTRA_MONITOR_ENABLED = "com.avrahamart.nightscreen.EXTRA_MONITOR_ENABLED";
    private static volatile boolean lockMonitoringActive = false;
    private boolean receiverRegistered = false;
    private AutoStartOnPowerReceiver lockReceiver;

    static boolean isLockMonitoringActive() {
        return lockMonitoringActive;
    }

    /**
     * Starts monitoring only while the user has enabled automatic lock activation
     * and the background-activity-launch permission prerequisite is satisfied.
     */
    public static void sync(Context context) {
        Context appContext = context.getApplicationContext();
        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(appContext);
        boolean enabled = snapshot != null
                && (snapshot.autoStartOnDeviceLock || snapshot.autoStartOnWake);
        if (enabled && (Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(appContext))) {
            Intent intent = new Intent(appContext, LockScreenMonitorService.class)
                    .putExtra(EXTRA_MONITOR_ENABLED, true);
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
        AutoStartNotificationController.sync(appContext);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        updateLockReceiverRegistration();
    }

    private void updateLockReceiverRegistration() {
        if (receiverRegistered && lockReceiver != null) {
            try {
                unregisterReceiver(lockReceiver);
            } catch (Throwable ignored) {
            }
            receiverRegistered = false;
        }

        lockReceiver = new AutoStartOnPowerReceiver();
        IntentFilter filter = new IntentFilter();
        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        boolean lockStart = snapshot != null && snapshot.autoStartOnDeviceLock;
        boolean wakeStart = snapshot != null && snapshot.autoStartOnWake;
        if (lockStart) filter.addAction(Intent.ACTION_USER_PRESENT);
        if (lockStart || wakeStart) {
            // Screen-off is also the reference point used to distinguish deep sleep
            // from a display-only off/on cycle.
            filter.addAction(Intent.ACTION_SCREEN_OFF);
        }
        if (wakeStart) filter.addAction(Intent.ACTION_SCREEN_ON);
        // NotificationListenerService owns charger detection whenever its
        // permission is granted; otherwise the lock-monitor process is the
        // persistent fallback when automatic activation on lock is enabled.
        if (!NightScreenApplication.hasNotificationAccess(this)) {
            filter.addAction(Intent.ACTION_POWER_CONNECTED);
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(lockReceiver, filter, RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(lockReceiver, filter);
            }
            receiverRegistered = true;
        } catch (Throwable ignored) {
            lockReceiver = null;
            receiverRegistered = false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        boolean enabled = intent != null && intent.hasExtra(EXTRA_MONITOR_ENABLED)
                ? intent.getBooleanExtra(EXTRA_MONITOR_ENABLED, false)
                : snapshot != null
                    && (snapshot.autoStartOnDeviceLock || snapshot.autoStartOnWake);
        if (!enabled || (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(this))) {
            lockMonitoringActive = false;
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        cancelScheduledRestart();
        updateLockReceiverRegistration();
        Notification notification = buildQuietNotification();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            lockMonitoringActive = true;
        } catch (Throwable ex) {
            lockMonitoringActive = false;
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
                CHANNEL_ID, "שירות ניטור פנימי", NotificationManager.IMPORTANCE_MIN);
        channel.setDescription("התראה מתמשכת לשירות ההפעלה האוטומטית בנעילת המכשיר");
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
        // This is only the Android-required technical foreground-service entry.
        // The separate normal notification is the visible, user-facing one and
        // opens the overlay-permission settings page.
        builder.setSmallIcon(R.drawable.ic_notification_transparent)
                .setContentTitle("שירות ניטור פנימי")
                .setContentText("פעיל")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_SECRET);
        builder.setPriority(Notification.PRIORITY_MIN)
                .setSound(null)
                .setVibrate(new long[0])
                .setDefaults(0);
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        return notification;
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
        lockMonitoringActive = false;
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
        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        if (snapshot == null
                || (!snapshot.autoStartOnDeviceLock && !snapshot.autoStartOnWake)) return;
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
