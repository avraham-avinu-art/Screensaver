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
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.provider.Settings;

public final class LockScreenMonitorService extends Service {
    private static final String TAG = "ScreensaverMonitor";
    // Versioned channel ID: a new ID makes Android apply the current silent,
    // minimum-importance and lockscreen-hidden defaults instead of retaining v4 settings.
    private static final String CHANNEL_ID = "screensaver_lock_monitor_v5";
    static final String ACTION_SYNC_REQUEST =
            "com.avrahamart.nightscreen.action.MONITOR_SYNC";
    static final String ACTION_ALARM_RESTART =
            "com.avrahamart.nightscreen.action.MONITOR_ALARM_RESTART";
    private static final int NOTIFICATION_ID = 6721;
    private static final int RESTART_PENDING_INTENT_ID = 6722;
    private static final long RESTART_DELAY_MS = 2_000L;
    private static volatile boolean lockMonitoringActive = false;
    private boolean receiverRegistered = false;
    private AutoStartOnPowerReceiver lockReceiver;

    static boolean isLockMonitoringActive() {
        return lockMonitoringActive;
    }

    static boolean isMonitoringAllowed(Context context) {
        MonitorSettingsSnapshot state = MonitorSettingsSnapshot.read(context.getApplicationContext());
        if (state == null || !state.needsMonitorService()) return false;
        boolean overlayRequired = state.autoStartOnDeviceLock || state.autoStartOnBoot
                || state.autoStartOnWake
                || (state.autoStartOnCharge && !state.useAndroidDreamForCharging);
        return !overlayRequired || Build.VERSION.SDK_INT < 29
                || Settings.canDrawOverlays(context.getApplicationContext());
    }

    /**
     * Keeps one foreground monitor/notification alive while any auto-start mode
     * is enabled. Dynamic receiver subscriptions remain independently gated.
     */
    static void restoreForegroundNotification(Context context) {
        Context appContext = context.getApplicationContext();
        boolean monitoringAllowed = isMonitoringAllowed(appContext);
        MonitorDiagnostics.record(appContext, "foreground notification restore check; monitoringAllowed="
                + monitoringAllowed + ", caller=" + describeCaller());
        if (monitoringAllowed) {
            MonitorSettingsSnapshot.setNotificationDetached(appContext, false);
        }
    }

    private static String describeCaller() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement frame : stack) {
            String className = frame.getClassName();
            if (className.startsWith("com.avrahamart.nightscreen.")
                    && !className.equals(LockScreenMonitorService.class.getName())) {
                return className + "#" + frame.getMethodName();
            }
        }
        return "unknown";
    }

    public static void sync(Context context) {
        Context appContext = context.getApplicationContext();
        boolean monitoringAllowed = isMonitoringAllowed(appContext);
        String caller = describeCaller();
        MonitorDiagnostics.record(appContext, "service sync invoked; monitoringAllowed="
                + monitoringAllowed + ", caller=" + caller);
        if (monitoringAllowed) {
            Intent intent = new Intent(appContext, LockScreenMonitorService.class);
            intent.setAction(ACTION_SYNC_REQUEST);
            MonitorDiagnostics.record(appContext, "service start request dispatched; source=APP_SYNC"
                    + ", caller=" + caller);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent);
                } else {
                    appContext.startService(intent);
                }
            } catch (Throwable error) {
                Log.e(TAG, "sync: startForegroundService/startService rejected", error);
                MonitorDiagnostics.record(appContext, "sync rejected: " + error);
            }
        } else {
            try {
                appContext.stopService(new Intent(appContext, LockScreenMonitorService.class));
            } catch (Throwable ignored) {
            }
            // The service may already have been destroyed while its detached
            // notification is still posted. Explicitly remove that standalone entry.
            try {
                NotificationManager manager = (NotificationManager)
                        appContext.getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) manager.cancel(NOTIFICATION_ID);
            } catch (Throwable ignored) {
            }
            MonitorSettingsSnapshot.setNotificationDetached(appContext, false);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MonitorDiagnostics.record(this, "service onCreate");
        createNotificationChannel();
        updateLockReceiverRegistration();
    }

    private void updateLockReceiverRegistration() {
        if (receiverRegistered && lockReceiver != null) {
            lockReceiver.cancelPendingLockCheck();
            try {
                unregisterReceiver(lockReceiver);
            } catch (Throwable ignored) {
            }
            receiverRegistered = false;
        }

        lockReceiver = new AutoStartOnPowerReceiver();
        IntentFilter filter = new IntentFilter();
        MonitorSettingsSnapshot state = MonitorSettingsSnapshot.read(this);
        if (state != null && AutoStartPolicy.isEnabledForAction(
                Intent.ACTION_SCREEN_OFF, state.autoStartOnDeviceLock,
                false, false, false, false)) {
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
        }
        if (state != null && AutoStartPolicy.isEnabledForAction(
                Intent.ACTION_SCREEN_ON, false, false,
                state.autoStartOnWake, false, false)) {
            filter.addAction(Intent.ACTION_SCREEN_ON);
        }
        // This service is the only app-managed charging listener. Android Dream
        // owns that event when the system screensaver route is selected.
        if (state != null && AutoStartPolicy.isEnabledForAction(
                Intent.ACTION_POWER_CONNECTED, false, false, false,
                state.autoStartOnCharge, state.useAndroidDreamForCharging)) {
            filter.addAction(Intent.ACTION_POWER_CONNECTED);
        }
        if (filter.countActions() == 0) {
            lockReceiver = null;
            receiverRegistered = false;
            return;
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
        String action = intent == null ? null : intent.getAction();
        String startSource;
        if (intent == null) {
            startSource = "ANDROID_STICKY_RESTART_OR_NULL_INTENT";
        } else if (ACTION_ALARM_RESTART.equals(action)) {
            startSource = "ALARM_MANAGER_RECEIVER";
        } else if (ACTION_SYNC_REQUEST.equals(action)) {
            startSource = "APP_SYNC";
        } else {
            startSource = "OTHER_EXPLICIT_OR_LEGACY_START";
        }
        MonitorDiagnostics.record(this, "onStartCommand; startId=" + startId
                + ", startSource=" + startSource + ", action=" + action
                + ", flags=" + flags
                + ", elapsedRealtime=" + SystemClock.elapsedRealtime());
        if (!isMonitoringAllowed(this)) {
            MonitorDiagnostics.record(this, "monitoring not allowed; stopping service");
            lockMonitoringActive = false;
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        MonitorSettingsSnapshot state = MonitorSettingsSnapshot.read(this);
        // Migrate installations that used the old detached-notification behavior.
        // The notification now remains attached to this independent foreground service.
        MonitorSettingsSnapshot.setNotificationDetached(this, false);

        // If the service was restarted after the user unlocked the phone, clear
        // a stale one-per-lock-cycle latch. Do not clear it while the device is
        // actually locked.
        if (state != null && state.autoStartOnDeviceLock) {
            try {
                android.app.KeyguardManager keyguard =
                        (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                if (keyguard != null && !keyguard.isDeviceLocked()) {
                    MonitorSettingsSnapshot.resetDeviceLockStart(this);
                }
            } catch (Throwable ignored) {
            }
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
            MonitorDiagnostics.record(this, "startForeground succeeded; notificationId=" + NOTIFICATION_ID);
            MonitorDiagnostics.recordNotificationState(this, "immediately after startForeground");
        } catch (Throwable ex) {
            Log.e(TAG, "startForeground failed", ex);
            MonitorDiagnostics.record(this, "startForeground failed: " + ex);
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
                CHANNEL_ID, "הפעלה אוטומטית של שומר מסך", NotificationManager.IMPORTANCE_MIN);
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
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, NOTIFICATION_ID, launchIntent, pendingFlags);

        // KeyMapper-style notification semantics: ongoing and silent, with an action
        // that opens the app; it is not tied to the settings Activity lifecycle.
        builder.setSmallIcon(R.drawable.ic_notification_transparent)
                .setContentTitle("הפעלה אוטומטית של שומר מסך")
                .setContentText("השירות פועל ברקע לפי הגדרות ההפעלה האוטומטית.")
                .setContentIntent(contentIntent)
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
        // Keep the foreground notification non-clearable, including on Android 13+.
        notification.flags |= Notification.FLAG_NO_CLEAR;
        return notification;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        MonitorDiagnostics.record(this, "onTaskRemoved; monitoringAllowed=" + isMonitoringAllowed(this)
                + ", foregroundActive=" + lockMonitoringActive
                + ", rootIntentAction=" + (rootIntent == null ? "null" : rootIntent.getAction())
                + ", rootIntentComponent=" + (rootIntent == null ? "null" : rootIntent.getComponent())
                + ", elapsedRealtime=" + SystemClock.elapsedRealtime());
        MonitorDiagnostics.recordNotificationState(this, "onTaskRemoved entry");
        MonitorDiagnostics.scheduleTaskRemovedProbes(this);
        // The monitor is independent of the UI task. Some device builds may still
        // tear down background processes after the recent-task card is dismissed.
        // Schedule a single recovery check; the same notification ID is reused,
        // and a live service simply refreshes its existing foreground notification.
        if (isMonitoringAllowed(this)) {
            scheduleRestartIfEnabled();
        } else {
            removeForegroundNotification();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        MonitorDiagnostics.record(this, "onDestroy; monitoringAllowed=" + isMonitoringAllowed(this)
                + ", elapsedRealtime=" + SystemClock.elapsedRealtime());
        MonitorDiagnostics.recordNotificationState(this, "onDestroy entry");
        lockMonitoringActive = false;
        if (!isMonitoringAllowed(this)) {
            removeForegroundNotification();
        }
        // Do not demote or detach the notification when the UI closes. If Android
        // destroys the service independently, recover it only while auto-start is enabled.
        scheduleRestartIfEnabled();
        if (receiverRegistered && lockReceiver != null) {
            lockReceiver.cancelPendingLockCheck();
            try {
                unregisterReceiver(lockReceiver);
            } catch (Throwable ignored) {
            }
        }
        super.onDestroy();
    }

    private void removeForegroundNotification() {
        MonitorSettingsSnapshot.setNotificationDetached(this, false);
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
        } catch (Throwable ignored) {
        }
        // If the notification was already detached, stopForeground(REMOVE) no
        // longer owns it. Cancel by ID as well when all automatic-start modes stop.
        try {
            NotificationManager manager = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.cancel(NOTIFICATION_ID);
        } catch (Throwable ignored) {
        }
    }

    private void scheduleRestartIfEnabled() {
        if (!isMonitoringAllowed(this)) {
            MonitorDiagnostics.record(this, "scheduleRestart skipped: monitoring disabled");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            MonitorDiagnostics.record(this, "restart alarm not scheduled: AlarmManager unavailable");
            return;
        }
        PendingIntent restartIntent = restartPendingIntent();
        if (restartIntent == null) {
            MonitorDiagnostics.record(this, "restart alarm not scheduled: PendingIntent unavailable");
            return;
        }
        try {
            long triggerAt = SystemClock.elapsedRealtime() + RESTART_DELAY_MS;
            if (Build.VERSION.SDK_INT >= 23) {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, restartIntent);
            } else {
                alarmManager.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, restartIntent);
            }
            MonitorDiagnostics.record(this, "restart alarm scheduled; delayMs=" + RESTART_DELAY_MS
                        + ", triggerElapsedRealtime=" + triggerAt
                        + ", alarmType=ELAPSED_REALTIME_WAKEUP");
        } catch (Throwable error) {
            Log.e(TAG, "restart alarm scheduling failed", error);
            MonitorDiagnostics.record(this, "restart alarm scheduling failed: " + error);
        }
    }

    private void cancelScheduledRestart() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent restartIntent = restartPendingIntent();
        if (alarmManager != null && restartIntent != null) {
            try {
                alarmManager.cancel(restartIntent);
                MonitorDiagnostics.record(this, "restart alarm cancelled by service start");
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
