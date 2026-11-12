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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;

public final class LockScreenMonitorService extends Service {
    private static final int NOTIFICATION_ID = AutoStartNotificationController.NOTIFICATION_ID;
    private static final int RESTART_PENDING_INTENT_ID = 6722;
    private static final long RESTART_DELAY_MS = 5_000L;
    private static final String ACTION_RESTART_AFTER_TASK_REMOVED =
            "com.avrahamart.nightscreen.ACTION_RESTART_MONITOR_AFTER_TASK_REMOVED";

    static final String EXTRA_MONITOR_ENABLED =
            "com.avrahamart.nightscreen.EXTRA_MONITOR_ENABLED";

    private static final long[] RECONCILE_RETRY_DELAYS_MS = {
            1_000L, 2_000L, 5_000L, 10_000L, 30_000L
    };

    private static volatile boolean lockMonitoringActive = false;

    private final Handler serviceHandler = new Handler(Looper.getMainLooper());

    private boolean receiverRegistered = false;
    private boolean foregroundStarted = false;
    private boolean restartScheduled = false;
    private boolean retryScheduled = false;
    private boolean monitorReady = false;
    private boolean destroying = false;
    private boolean lastKnownMonitoringRequested = false;
    private int retryAttempt = 0;
    private int latestStartId = 0;
    private String receiverFilterSignature;
    private AutoStartOnPowerReceiver lockReceiver;

    // Declared after the service state fields referenced by the callback.
    private final Runnable reconcileRetry = () -> {
        retryScheduled = false;
        if (!destroying && foregroundStarted) {
            reconcileFromPersistedSettings();
        }
    };

    static boolean isLockMonitoringActive() {
        return lockMonitoringActive;
    }

    /**
     * Reconciles the foreground service with the user's persisted opt-in.
     * A failed settings read is unknown state, not an instruction to stop.
     */
    public static void sync(Context context) {
        Context appContext = context.getApplicationContext();
        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(appContext);

        // Never tear down a working service because a cross-process settings read
        // failed temporarily. The started service will reconcile again itself.
        if (snapshot == null) return;

        boolean enabled = isMonitoringRequested(snapshot);
        boolean overlayGranted = Build.VERSION.SDK_INT < 29
                || Settings.canDrawOverlays(appContext);

        if (enabled && overlayGranted) {
            Intent intent = new Intent(appContext, LockScreenMonitorService.class)
                    .putExtra(EXTRA_MONITOR_ENABLED, true);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent);
                } else {
                    appContext.startService(intent);
                }
            } catch (Throwable ignored) {
                // A later Activity, settings change, boot event, or sticky restart
                // will reconcile again. Do not stop an existing service here.
            }
            return;
        }

        cancelScheduledRestart(appContext);
        try {
            appContext.stopService(new Intent(appContext, LockScreenMonitorService.class));
        } catch (Throwable ignored) {
            // The next successful reconciliation will apply the persisted state.
        }
    }

    private static boolean isMonitoringRequested(MonitorSettingsSnapshot snapshot) {
        return snapshot != null
                && (snapshot.autoStartOnDeviceLock || snapshot.autoStartOnWake);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AutoStartNotificationController.createChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        latestStartId = startId;

        // Establish foreground state before provider access or receiver registration.
        if (!promoteToForeground()) {
            lockMonitoringActive = false;
            monitorReady = false;
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        cancelScheduledRestart();
        cancelReconcileRetry();

        return reconcileFromPersistedSettings() ? START_STICKY : START_NOT_STICKY;
    }

    private boolean promoteToForeground() {
        try {
            Notification notification = AutoStartNotificationController.buildNotification(this);
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notification, 0);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            foregroundStarted = true;
            return true;
        } catch (Throwable ignored) {
            foregroundStarted = false;
            return false;
        }
    }

    /**
     * Returns true while the service should remain started. A temporarily
     * unavailable settings provider or a transient receiver-registration
     * failure keeps the foreground service alive and schedules a retry.
     */
    private boolean reconcileFromPersistedSettings() {
        if (destroying) return false;

        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        if (snapshot == null) {
            scheduleReconcileRetry();
            return true;
        }

        boolean requested = isMonitoringRequested(snapshot);
        lastKnownMonitoringRequested = requested;
        boolean overlayGranted = Build.VERSION.SDK_INT < 29
                || Settings.canDrawOverlays(this);

        if (!requested || !overlayGranted) {
            stopForDisabledOrUnavailableFeature();
            return false;
        }

        if (!updateLockReceiverRegistration(snapshot)) {
            monitorReady = false;
            lockMonitoringActive = false;
            scheduleReconcileRetry();
            return true;
        }

        monitorReady = true;
        lockMonitoringActive = true;
        retryAttempt = 0;
        cancelReconcileRetry();
        return true;
    }

    /**
     * Register the replacement filter before retiring the previous receiver.
     * That avoids a gap in SCREEN_OFF/SCREEN_ON delivery when settings change.
     * If registration fails, the previous receiver remains registered while
     * the service retries; activation is held disabled until the new filter is
     * installed successfully.
     */
    private boolean updateLockReceiverRegistration(MonitorSettingsSnapshot snapshot) {
        boolean lockStart = snapshot.autoStartOnDeviceLock;
        boolean wakeStart = snapshot.autoStartOnWake;
        boolean notificationAccess = NightScreenApplication.hasNotificationAccess(this);
        boolean chargerFallback = !notificationAccess
                && snapshot.autoStartOnCharge
                && !snapshot.useAndroidDreamForCharging;

        String filterSignature = "lock=" + lockStart
                + ";wake=" + wakeStart
                + ";chargerFallback=" + chargerFallback
                + ";notificationAccess=" + notificationAccess;

        if (receiverRegistered && lockReceiver != null
                && filterSignature.equals(receiverFilterSignature)) {
            return true;
        }

        AutoStartOnPowerReceiver replacement = new AutoStartOnPowerReceiver();
        IntentFilter filter = new IntentFilter();

        if (lockStart) filter.addAction(Intent.ACTION_USER_PRESENT);
        if (lockStart || wakeStart) filter.addAction(Intent.ACTION_SCREEN_OFF);
        if (wakeStart) filter.addAction(Intent.ACTION_SCREEN_ON);
        if (chargerFallback) filter.addAction(Intent.ACTION_POWER_CONNECTED);

        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(replacement, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(replacement, filter);
            }
        } catch (RuntimeException ignored) {
            replacement.deactivate();
            return false;
        }

        AutoStartOnPowerReceiver previous = lockReceiver;
        boolean previousWasRegistered = receiverRegistered;

        lockReceiver = replacement;
        receiverRegistered = true;
        receiverFilterSignature = filterSignature;

        if (previousWasRegistered && previous != null) {
            previous.deactivate();
            try {
                unregisterReceiver(previous);
            } catch (RuntimeException ignored) {
                // A retired receiver is disabled, so it cannot cause duplicate activations.
            }
        }

        return true;
    }

    private void stopForDisabledOrUnavailableFeature() {
        cancelReconcileRetry();
        cancelScheduledRestart();
        unregisterLockReceiver();
        monitorReady = false;
        lockMonitoringActive = false;

        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            foregroundStarted = false;
        }

        if (latestStartId > 0) {
            stopSelfResult(latestStartId);
        } else {
            stopSelf();
        }
    }

    private void unregisterLockReceiver() {
        if (lockReceiver == null) {
            receiverRegistered = false;
            receiverFilterSignature = null;
            return;
        }

        lockReceiver.deactivate();
        if (receiverRegistered) {
            try {
                unregisterReceiver(lockReceiver);
            } catch (RuntimeException ignored) {
                // The platform may already have removed it during process cleanup.
            }
        }

        lockReceiver = null;
        receiverRegistered = false;
        receiverFilterSignature = null;
    }

    private void scheduleReconcileRetry() {
        if (destroying || !foregroundStarted || retryScheduled) return;

        int index = Math.min(retryAttempt, RECONCILE_RETRY_DELAYS_MS.length - 1);
        long delay = RECONCILE_RETRY_DELAYS_MS[index];
        if (retryAttempt < RECONCILE_RETRY_DELAYS_MS.length - 1) {
            retryAttempt++;
        }

        retryScheduled = serviceHandler.postDelayed(reconcileRetry, delay);
    }

    private void cancelReconcileRetry() {
        serviceHandler.removeCallbacks(reconcileRetry);
        retryScheduled = false;
        retryAttempt = 0;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // The monitor's recovery hook belongs to the screensaver task only.
        // Removing the independent Settings task must not schedule a monitor restart.
        if (rootIntent == null || rootIntent.getComponent() == null
                || !MainActivity.class.getName().equals(
                        rootIntent.getComponent().getClassName())) {
            super.onTaskRemoved(rootIntent);
            return;
        }

        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        boolean requested = snapshot == null
                ? lastKnownMonitoringRequested
                : isMonitoringRequested(snapshot);

        if (snapshot != null) {
            lastKnownMonitoringRequested = requested;
        }

        boolean overlayGranted = Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this);
        if (requested && overlayGranted) {
            if (!foregroundStarted) promoteToForeground();
            // stopWithTask=false keeps a started service alive on standard Android.
            // The delayed start is a secondary recovery route for task managers
            // that remove the service together with its task.
            scheduleRestartIfEnabled();
        }

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        destroying = true;
        cancelReconcileRetry();

        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        boolean requested = snapshot == null
                ? lastKnownMonitoringRequested
                : isMonitoringRequested(snapshot);
        if (snapshot != null) {
            lastKnownMonitoringRequested = requested;
        }

        monitorReady = false;
        lockMonitoringActive = false;
        unregisterLockReceiver();

        boolean overlayGranted = Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this);
        if (requested && overlayGranted) {
            scheduleRestartIfEnabled();
        }

        foregroundStarted = false;
        super.onDestroy();
    }

    private void scheduleRestartIfEnabled() {
        if (restartScheduled) return;

        MonitorSettingsSnapshot snapshot = MonitorSettingsSnapshot.read(this);
        boolean requested = snapshot == null
                ? lastKnownMonitoringRequested
                : isMonitoringRequested(snapshot);
        if (snapshot != null) {
            lastKnownMonitoringRequested = requested;
        }

        if (!requested) return;
        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(this)) return;

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent restartIntent = restartPendingIntent(this);
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
            restartScheduled = true;
        } catch (Throwable ignored) {
            // START_STICKY remains the primary process-death recovery mechanism.
        }
    }

    private void cancelScheduledRestart() {
        cancelScheduledRestart(this);
        restartScheduled = false;
    }

    private static void cancelScheduledRestart(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent restartIntent = restartPendingIntent(context);
        if (alarmManager == null || restartIntent == null) return;

        try {
            alarmManager.cancel(restartIntent);
        } catch (Throwable ignored) {
            // A missing alarm is already the desired state.
        }
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
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
