package com.avrahamart.nightscreen;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

public final class LockScreenMonitorService extends Service {
    private static final String TAG = "NightScreenMonitor";
    private static final int NOTIFICATION_ID = AutoStartNotificationController.NOTIFICATION_ID;
    private static final int RESTART_PENDING_INTENT_ID = 6722;
    // Match the recovery delay used by OwnTracks: give vendor task-cleanup time to finish.
    private static final long RESTART_DELAY_MS = 5_000L;
    private static final String ACTION_RESTART_AFTER_TASK_REMOVED =
            "com.avrahamart.nightscreen.ACTION_RESTART_MONITOR_AFTER_TASK_REMOVED";
    static final String EXTRA_MONITOR_ENABLED =
            "com.avrahamart.nightscreen.EXTRA_MONITOR_ENABLED";

    private static volatile boolean lockMonitoringActive = false;
    private boolean receiverRegistered = false;
    private boolean restartScheduled = false;
    private boolean monitorReady = false;
    private String receiverFilterSignature;
    private AutoStartOnPowerReceiver lockReceiver;

    static boolean isLockMonitoringActive() {
        return lockMonitoringActive;
    }

    /**
     * Starts the monitor only while at least one supported automatic activation
     * mode is enabled and its overlay-permission prerequisite is available.
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
                Log.i(TAG, "Monitor start requested. lock="
                        + snapshot.autoStartOnDeviceLock + ", wake=" + snapshot.autoStartOnWake);
            } catch (Throwable error) {
                Log.e(TAG, "Android rejected the monitor start request.", error);
            }
        } else {
            Log.i(TAG, "Monitor not started. settingsAvailable=" + (snapshot != null)
                    + ", enabled=" + enabled
                    + ", overlayGranted="
                    + (Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(appContext)));
            cancelScheduledRestart(appContext);
            try {
                appContext.stopService(new Intent(appContext, LockScreenMonitorService.class));
            } catch (Throwable error) {
                Log.e(TAG, "Unable to stop the monitor after its settings were disabled.", error);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AutoStartNotificationController.createChannel(this);
        Log.i(TAG, "Monitor service created.");
    }

    private void updateLockReceiverRegistration() {
        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        boolean lockStart = snapshot != null && snapshot.autoStartOnDeviceLock;
        boolean wakeStart = snapshot != null && snapshot.autoStartOnWake;
        boolean notificationAccess = NightScreenApplication.hasNotificationAccess(this);
        String filterSignature = "lock=" + lockStart
                + ";wake=" + wakeStart
                + ";notificationAccess=" + notificationAccess;

        // Repeated sync requests are normal when activities resume. Do not
        // unregister/re-register an unchanged filter: that creates a gap in
        // which SCREEN_OFF or SCREEN_ON can be missed.
        if (receiverRegistered && lockReceiver != null
                && filterSignature.equals(receiverFilterSignature)) {
            return;
        }

        monitorReady = false;
        lockMonitoringActive = false;
        if (receiverRegistered && lockReceiver != null) {
            try {
                unregisterReceiver(lockReceiver);
            } catch (RuntimeException error) {
                Log.w(TAG, "Previous monitor receiver could not be unregistered cleanly.", error);
            }
            receiverRegistered = false;
            receiverFilterSignature = null;
        }

        lockReceiver = new AutoStartOnPowerReceiver();
        IntentFilter filter = new IntentFilter();
        if (lockStart) filter.addAction(Intent.ACTION_USER_PRESENT);
        if (lockStart || wakeStart) {
            filter.addAction(Intent.ACTION_SCREEN_OFF);
        }
        if (wakeStart) filter.addAction(Intent.ACTION_SCREEN_ON);
        // When notification-listener access is absent, this process is the
        // persistent fallback for charger broadcasts as well.
        if (!notificationAccess) {
            filter.addAction(Intent.ACTION_POWER_CONNECTED);
        }

        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(lockReceiver, filter, RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(lockReceiver, filter);
            }
            receiverRegistered = true;
            receiverFilterSignature = filterSignature;
            Log.i(TAG, "Monitor event receiver registered. lock=" + lockStart
                    + ", wake=" + wakeStart
                    + ", notificationAccess=" + notificationAccess);
        } catch (RuntimeException error) {
            lockReceiver = null;
            receiverRegistered = false;
            receiverFilterSignature = null;
            Log.e(TAG, "Failed to register the monitor event receiver.", error);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        Log.i(TAG, "onStartCommand received. intentNull=" + (intent == null)
                + ", action=" + action + ", startId=" + startId);

        // Promote immediately. This is deliberately before receiver registration
        // or other setup, as required for startForegroundService callers.
        try {
            Notification notification = buildQuietNotification();
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notification, 0);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Throwable error) {
            lockMonitoringActive = false;
            Log.e(TAG, "startForeground failed; monitor cannot remain active.", error);
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        cancelScheduledRestart();

        // Persisted user settings are authoritative, including for delayed recovery
        // PendingIntents created before the user disabled automatic monitoring.
        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        boolean enabled = snapshot != null
                && (snapshot.autoStartOnDeviceLock || snapshot.autoStartOnWake);
        boolean overlayGranted = Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this);
        if (!enabled || !overlayGranted) {
            lockMonitoringActive = false;
            Log.w(TAG, "Foreground service started but monitoring is disabled. enabled="
                    + enabled + ", overlayGranted=" + overlayGranted);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        updateLockReceiverRegistration();
        monitorReady = receiverRegistered;
        lockMonitoringActive = monitorReady;
        if (!monitorReady) {
            Log.e(TAG, "Monitor startup failed: foreground service started, but event receiver registration failed.");
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        Log.i(TAG, "Monitor is active. receiverRegistered=true, sticky restart enabled.");
        return START_STICKY;
    }

    private Notification buildQuietNotification() {
        return AutoStartNotificationController.buildNotification(this);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // A launcher-task removal must not downgrade or drop the running FGS.
        // Reassert the same persistent notification immediately, as in the
        // task-removal handling used by ForegroundServiceExample.
        if (monitorReady && receiverRegistered) {
            try {
                Notification notification = buildQuietNotification();
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(NOTIFICATION_ID, notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                } else if (Build.VERSION.SDK_INT >= 29) {
                    startForeground(NOTIFICATION_ID, notification, 0);
                } else {
                    startForeground(NOTIFICATION_ID, notification);
                }
            } catch (RuntimeException error) {
                Log.e(TAG, "Unable to reassert foreground state after task removal.", error);
            }
        }
        scheduleRestartIfEnabled("task-removed");
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        boolean wasReady = monitorReady;
        monitorReady = false;
        lockMonitoringActive = false;
        Log.w(TAG, "Monitor service destroyed. Checking whether monitoring is still requested.");
        if (wasReady) {
            scheduleRestartIfEnabled("service-destroyed");
        }
        if (receiverRegistered && lockReceiver != null) {
            try {
                unregisterReceiver(lockReceiver);
            } catch (Throwable error) {
                Log.w(TAG, "Monitor receiver cleanup failed.", error);
            }
            receiverRegistered = false;
            receiverFilterSignature = null;
        }
        super.onDestroy();
    }

    private void scheduleRestartIfEnabled(String reason) {
        if (restartScheduled) {
            Log.d(TAG, "Recovery alarm already scheduled; reason=" + reason);
            return;
        }

        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        if (snapshot == null
                || (!snapshot.autoStartOnDeviceLock && !snapshot.autoStartOnWake)) {
            Log.i(TAG, "No recovery scheduled because automatic monitoring is disabled.");
            return;
        }
        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "No recovery scheduled because overlay permission is missing.");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "No recovery scheduled: AlarmManager is unavailable.");
            return;
        }

        PendingIntent restartIntent = restartPendingIntent();
        if (restartIntent == null) {
            Log.e(TAG, "No recovery scheduled: foreground-service PendingIntent creation failed.");
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
            restartScheduled = true;
            Log.i(TAG, "Direct foreground-service recovery scheduled in "
                    + RESTART_DELAY_MS + "ms; reason=" + reason);
        } catch (Throwable error) {
            Log.e(TAG, "Failed to schedule foreground-service recovery; reason=" + reason, error);
        }
    }

    private void cancelScheduledRestart() {
        cancelScheduledRestart(this);
        restartScheduled = false;
        Log.d(TAG, "Pending monitor recovery alarm cancelled after service start.");
    }

    private static void cancelScheduledRestart(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent restartIntent = restartPendingIntent(context);
        if (alarmManager == null || restartIntent == null) return;
        try {
            alarmManager.cancel(restartIntent);
            Log.i(TAG, "Pending monitor recovery alarm cancelled.");
        } catch (Throwable error) {
            Log.e(TAG, "Unable to cancel pending monitor recovery alarm.", error);
        }
    }

    /**
     * OwnTracks-style restart route: the system alarm starts this foreground
     * service directly. Persisted settings are rechecked when it arrives.
     */
    private PendingIntent restartPendingIntent() {
        return restartPendingIntent(this);
    }

    private static PendingIntent restartPendingIntent(Context context) {
        Intent intent = new Intent(context, LockScreenMonitorService.class)
                .setAction(ACTION_RESTART_AFTER_TASK_REMOVED);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return PendingIntent.getForegroundService(
                        context, RESTART_PENDING_INTENT_ID, intent, pendingFlags);
            }
            return PendingIntent.getService(
                    context, RESTART_PENDING_INTENT_ID, intent, pendingFlags);
        } catch (Throwable error) {
            Log.e(TAG, "Unable to create direct foreground-service PendingIntent.", error);
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
