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
    // True only after the event receiver was successfully registered.
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
                String message = "Monitor start request dispatched. lock="
                        + snapshot.autoStartOnDeviceLock + ", wake=" + snapshot.autoStartOnWake;
                Log.i(TAG, message);
                MonitorDiagnostics.record(appContext, message);
            } catch (RuntimeException error) {
                String message = "Monitor start request rejected; exception="
                        + error.getClass().getName() + ": " + error.getMessage()
                        + ", sdk=" + Build.VERSION.SDK_INT
                        + ", targetSdk=" + appContext.getApplicationInfo().targetSdkVersion;
                Log.e(TAG, message, error);
                MonitorDiagnostics.record(appContext, message);
            }
        } else {
            String message = "Monitor not started. settingsAvailable=" + (snapshot != null)
                    + ", enabled=" + enabled
                    + ", overlayGranted="
                    + (Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(appContext));
            Log.i(TAG, message);
            MonitorDiagnostics.record(appContext, message);
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
        Log.i(TAG, "Monitor service created. pid=" + android.os.Process.myPid());
        MonitorDiagnostics.record(this, "monitor service onCreate; pid="
                + android.os.Process.myPid() + ", sdk=" + Build.VERSION.SDK_INT
                + ", targetSdk=" + getApplicationInfo().targetSdkVersion);
    }

    private void updateLockReceiverRegistration() {
        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        boolean lockStart = snapshot != null && snapshot.autoStartOnDeviceLock;
        boolean wakeStart = snapshot != null && snapshot.autoStartOnWake;
        boolean notificationAccess = NightScreenApplication.hasNotificationAccess(this);
        String filterSignature = "lock=" + lockStart
                + ";wake=" + wakeStart
                + ";notificationAccess=" + notificationAccess;

        // sync() may be called repeatedly from Activity onResume and permission
        // callbacks. Keep the existing registration when its event set is unchanged:
        // unregister/register here would create a gap in which SCREEN_OFF could be lost.
        if (receiverRegistered && lockReceiver != null
                && filterSignature.equals(receiverFilterSignature)) {
            monitorReady = true;
            lockMonitoringActive = true;
            MonitorDiagnostics.record(this, "Monitor event receiver unchanged; keeping registration. "
                    + filterSignature);
            return;
        }

        monitorReady = false;
        lockMonitoringActive = false;
        if (receiverRegistered && lockReceiver != null) {
            try {
                unregisterReceiver(lockReceiver);
            } catch (RuntimeException error) {
                MonitorDiagnostics.record(this, "Previous monitor receiver unregister failed; exception="
                        + error.getClass().getName() + ": " + error.getMessage());
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
            String message = "Monitor event receiver registered. lock=" + lockStart
                    + ", wake=" + wakeStart
                    + ", notificationAccess=" + notificationAccess
                    + ", actions=" + filter.countActions();
            Log.i(TAG, message);
            MonitorDiagnostics.record(this, message);
        } catch (RuntimeException error) {
            lockReceiver = null;
            receiverRegistered = false;
            receiverFilterSignature = null;
            String message = "Failed to register monitor event receiver; exception="
                    + error.getClass().getName() + ": " + error.getMessage()
                    + ", sdk=" + Build.VERSION.SDK_INT;
            Log.e(TAG, message, error);
            MonitorDiagnostics.record(this, message);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        String startMessage = "onStartCommand received. intentNull=" + (intent == null)
                + ", action=" + action + ", flags=" + flags + ", startId=" + startId
                + ", pid=" + android.os.Process.myPid();
        Log.i(TAG, startMessage);
        MonitorDiagnostics.record(this, startMessage);

        // Promote immediately. This is deliberately before receiver registration
        // or other setup, as required for startForegroundService callers.
        try {
            Notification notification = buildQuietNotification();
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                // The specialUse type was introduced in API 34. On earlier
                // releases use the two-argument overload and the manifest's
                // legacy-compatible foreground-service declaration.
                startForeground(NOTIFICATION_ID, notification);
            }
            MonitorDiagnostics.record(this, "startForeground succeeded; notificationId="
                    + NOTIFICATION_ID + ", fgsType="
                    + (Build.VERSION.SDK_INT >= 34 ? "specialUse" : "manifest/default"));
            MonitorDiagnostics.recordNotificationState(this, "immediately after startForeground");
        } catch (RuntimeException error) {
            lockMonitoringActive = false;
            monitorReady = false;
            String message = "startForeground failed; exception=" + error.getClass().getName()
                    + ": " + error.getMessage() + ", sdk=" + Build.VERSION.SDK_INT
                    + ", targetSdk=" + getApplicationInfo().targetSdkVersion
                    + ", notificationPermissionEnabled=" + notificationsEnabled();
            Log.e(TAG, message, error);
            MonitorDiagnostics.record(this, message);
            MonitorDiagnostics.recordNotificationState(this, "startForeground failure");
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        cancelScheduledRestart();
        MonitorDiagnostics.record(this, "pending recovery alarm cancelled at service start");

        // Persisted user settings are authoritative, including for delayed recovery
        // PendingIntents created before the user disabled automatic monitoring.
        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        boolean enabled = snapshot != null
                && (snapshot.autoStartOnDeviceLock || snapshot.autoStartOnWake);
        boolean overlayGranted = Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this);
        if (!enabled || !overlayGranted) {
            lockMonitoringActive = false;
            String message = "Foreground service started but monitoring is disabled. enabled="
                    + enabled + ", overlayGranted=" + overlayGranted;
            Log.w(TAG, message);
            MonitorDiagnostics.record(this, message);
            monitorReady = false;
            lockMonitoringActive = false;
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        updateLockReceiverRegistration();
        monitorReady = receiverRegistered;
        lockMonitoringActive = monitorReady;
        if (!monitorReady) {
            String message = "Monitor startup failed: foreground notification is active, but "
                    + "the lock/wake event receiver was not registered. Service will stop "
                    + "instead of reporting a false active state.";
            Log.e(TAG, message);
            MonitorDiagnostics.record(this, message);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        String message = "Monitor is fully active. receiverRegistered=true, sticky restart enabled.";
        Log.i(TAG, message);
        MonitorDiagnostics.record(this, message);
        MonitorDiagnostics.recordNotificationState(this, "monitor fully initialized");
        return START_STICKY;
    }

    private Notification buildQuietNotification() {
        return AutoStartNotificationController.buildNotification(this);
    }

    private boolean notificationsEnabled() {
        try {
            android.app.NotificationManager manager =
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            return manager != null && manager.areNotificationsEnabled();
        } catch (RuntimeException error) {
            MonitorDiagnostics.record(this, "Unable to query notification permission: "
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
            return false;
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        String message = "onTaskRemoved received; monitorReady=" + monitorReady
                + ", receiverRegistered=" + receiverRegistered
                + ", pid=" + android.os.Process.myPid();
        Log.w(TAG, message);
        MonitorDiagnostics.record(this, message);
        MonitorDiagnostics.recordNotificationState(this, "onTaskRemoved callback");
        MonitorDiagnostics.scheduleTaskRemovedProbes(this);
        if (monitorReady) {
            scheduleRestartIfEnabled("task-removed");
        } else {
            MonitorDiagnostics.record(this, "Recovery not scheduled: monitor was not fully initialized.");
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        boolean wasReady = monitorReady;
        monitorReady = false;
        lockMonitoringActive = false;
        String message = "monitor service onDestroy; wasReady=" + wasReady
                + ", receiverRegistered=" + receiverRegistered
                + ", pid=" + android.os.Process.myPid();
        Log.w(TAG, message);
        MonitorDiagnostics.record(this, message);
        MonitorDiagnostics.recordNotificationState(this, "monitor service onDestroy");
        if (wasReady) {
            scheduleRestartIfEnabled("service-destroyed");
        } else {
            MonitorDiagnostics.record(this, "No recovery retry scheduled after failed/incomplete startup.");
        }
        if (receiverRegistered && lockReceiver != null) {
            try {
                unregisterReceiver(lockReceiver);
            } catch (Throwable error) {
                Log.w(TAG, "Monitor receiver cleanup failed.", error);
            }
            receiverRegistered = false;
        }
        super.onDestroy();
    }

    private void scheduleRestartIfEnabled(String reason) {
        if (restartScheduled) {
            String message = "Recovery alarm already scheduled; reason=" + reason;
            Log.d(TAG, message);
            MonitorDiagnostics.record(this, message);
            return;
        }

        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        if (snapshot == null
                || (!snapshot.autoStartOnDeviceLock && !snapshot.autoStartOnWake)) {
            Log.i(TAG, "No recovery scheduled because automatic monitoring is disabled.");
            MonitorDiagnostics.record(this, "No recovery scheduled: persisted monitor settings are disabled.");
            return;
        }
        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "No recovery scheduled because overlay permission is missing.");
            MonitorDiagnostics.record(this, "No recovery scheduled: overlay permission is missing.");
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
            String message = "Direct foreground-service recovery scheduled in "
                    + RESTART_DELAY_MS + "ms; reason=" + reason
                    + ", alarmType=ELAPSED_REALTIME_WAKEUP_SET_AND_ALLOW_WHILE_IDLE";
            Log.i(TAG, message);
            MonitorDiagnostics.record(this, message);
        } catch (RuntimeException error) {
            String message = "Failed to schedule foreground-service recovery; reason=" + reason
                    + ", exception=" + error.getClass().getName() + ": " + error.getMessage();
            Log.e(TAG, message, error);
            MonitorDiagnostics.record(this, message);
        }
    }

    private void cancelScheduledRestart() {
        cancelScheduledRestart(this);
        restartScheduled = false;
        Log.d(TAG, "Pending monitor recovery alarm cancelled after service start.");
        MonitorDiagnostics.record(this, "Pending monitor recovery alarm cancelled after service start.");
    }

    private static void cancelScheduledRestart(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent restartIntent = restartPendingIntent(context);
        if (alarmManager == null || restartIntent == null) return;
        try {
            alarmManager.cancel(restartIntent);
            Log.i(TAG, "Pending monitor recovery alarm cancelled.");
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to cancel pending monitor recovery alarm.", error);
            MonitorDiagnostics.record(context, "Unable to cancel recovery alarm; exception="
                    + error.getClass().getName() + ": " + error.getMessage());
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
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to create direct foreground-service PendingIntent.", error);
            MonitorDiagnostics.record(context, "Unable to create direct FGS PendingIntent; exception="
                    + error.getClass().getName() + ": " + error.getMessage());
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
