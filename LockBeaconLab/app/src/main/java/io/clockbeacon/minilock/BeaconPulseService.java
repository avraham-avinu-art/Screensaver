package io.clockbeacon.minilock;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PowerManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;

public final class BeaconPulseService extends Service {
    static final String ACTION_HIDE_CLOCK =
            "io.clockbeacon.minilock.event.HIDE_CLOCKFACE";

    private static final String NOTIFICATION_CHANNEL =
            "clock_beacon_engine";
    private static final int NOTIFICATION_ID = 8417;
    private static final int RECOVERY_REQUEST = 8418;
    private static final long RECOVERY_DELAY_MS = 5_000L;

    private static final long[] RETRY_DELAYS = {
            1_000L, 2_000L, 5_000L, 10_000L, 30_000L
    };

    private final Handler engineHandler = new Handler(Looper.getMainLooper());

    private BeaconScreenEventReceiver screenEvents;
    private boolean screenEventsRegistered;
    private boolean engineForeground;
    private boolean enabledConfirmed;
    private boolean retryPending;
    private boolean recoveryPending;
    private boolean destroying;
    private int retryIndex;
    private int wakeSequence;

    private final Runnable stateRetry = new Runnable() {
        @Override
        public void run() {
            retryPending = false;
            if (!destroying && engineForeground) reconcileEngineState();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createBeaconChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!promoteBeaconForeground()) {
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        cancelRecovery();
        cancelStateRetry();

        Boolean enabled = readEnabledState();
        if (enabled == null) {
            scheduleStateRetry();
            return START_STICKY;
        }

        if (!enabled) {
            shutDownEngine();
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        enabledConfirmed = true;
        if (!registerScreenEvents()) {
            scheduleStateRetry();
            return START_STICKY;
        }

        retryIndex = 0;
        return START_STICKY;
    }

    private boolean promoteBeaconForeground() {
        try {
            Notification notice = buildBeaconNotice();
            if (Build.VERSION.SDK_INT >= 34) {
                // targetSdk is intentionally 30 for the Android 11 device test.
                // On newer runtimes, this app uses the legacy untyped FGS path.
                startForeground(NOTIFICATION_ID, notice, 0);
            } else if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notice, 0);
            } else {
                startForeground(NOTIFICATION_ID, notice);
            }
            engineForeground = true;
            return true;
        } catch (RuntimeException error) {
            engineForeground = false;
            return false;
        }
    }

    private void reconcileEngineState() {
        Boolean enabled = readEnabledState();
        if (enabled == null) {
            scheduleStateRetry();
            return;
        }

        if (!enabled) {
            enabledConfirmed = false;
            shutDownEngine();
            stopSelf();
            return;
        }

        enabledConfirmed = true;
        if (!registerScreenEvents()) {
            scheduleStateRetry();
            return;
        }

        retryIndex = 0;
        cancelStateRetry();
    }

    private boolean registerScreenEvents() {
        if (screenEventsRegistered) return true;

        BeaconScreenEventReceiver candidate = new BeaconScreenEventReceiver(this);
        IntentFilter screenSignals = new IntentFilter();
        screenSignals.addAction(Intent.ACTION_SCREEN_ON);
        screenSignals.addAction(Intent.ACTION_SCREEN_OFF);
        screenSignals.addAction(Intent.ACTION_USER_PRESENT);

        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(candidate, screenSignals, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(candidate, screenSignals);
            }
            screenEvents = candidate;
            screenEventsRegistered = true;
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    void onDisplayAwake() {
        final int sequence = ++wakeSequence;
        attemptClockface(sequence, 0);
    }

    private void attemptClockface(int sequence, int attempt) {
        if (destroying || sequence != wakeSequence || !enabledConfirmed) return;

        PowerManager power =
                (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (power == null || !power.isInteractive()) return;

        KeyguardManager guard =
                (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (guard == null) return;

        if (guard.isKeyguardLocked()) {
            if (!Settings.canDrawOverlays(this)) return;

            Intent clockface = new Intent(this, BeaconLockfaceActivity.class);
            clockface.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try {
                startActivity(clockface);
            } catch (RuntimeException error) {
                if (attempt < 4) {
                    engineHandler.postDelayed(
                            () -> attemptClockface(sequence, attempt + 1), 300L);
                }
            }
            return;
        }

        if (attempt < 4) {
            engineHandler.postDelayed(
                    () -> attemptClockface(sequence, attempt + 1), 300L);
        }
    }

    void hideClockface() {
        wakeSequence++;
        Intent close = new Intent(ACTION_HIDE_CLOCK).setPackage(getPackageName());
        sendBroadcast(close);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Boolean enabled = readEnabledState();
        if (Boolean.TRUE.equals(enabled) || (enabled == null && enabledConfirmed)) {
            scheduleRecovery();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        destroying = true;
        cancelStateRetry();

        Boolean enabled = readEnabledState();
        boolean shouldRecover = Boolean.TRUE.equals(enabled)
                || (enabled == null && enabledConfirmed);

        wakeSequence++;
        hideClockface();
        unregisterScreenEvents();

        if (shouldRecover) {
            scheduleRecovery();
        } else {
            cancelRecovery();
        }

        engineForeground = false;
        super.onDestroy();
    }

    private void shutDownEngine() {
        cancelStateRetry();
        cancelRecovery();
        wakeSequence++;
        hideClockface();
        unregisterScreenEvents();

        if (engineForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            engineForeground = false;
        }
    }

    private void unregisterScreenEvents() {
        if (screenEvents == null) {
            screenEventsRegistered = false;
            return;
        }

        if (screenEventsRegistered) {
            try {
                unregisterReceiver(screenEvents);
            } catch (RuntimeException ignored) {
            }
        }

        screenEvents = null;
        screenEventsRegistered = false;
    }

    private void scheduleStateRetry() {
        if (destroying || !engineForeground || retryPending) return;

        int index = Math.min(retryIndex, RETRY_DELAYS.length - 1);
        long delay = RETRY_DELAYS[index];
        if (retryIndex < RETRY_DELAYS.length - 1) retryIndex++;

        retryPending = true;
        engineHandler.postDelayed(stateRetry, delay);
    }

    private void cancelStateRetry() {
        engineHandler.removeCallbacks(stateRetry);
        retryPending = false;
        retryIndex = 0;
    }

    private Boolean readEnabledState() {
        try {
            Bundle state = getContentResolver().call(
                    Uri.parse("content://" + BeaconControlProvider.AUTHORITY),
                    BeaconControlProvider.METHOD_FETCH, null, null);
            return state == null ? null
                    : state.getBoolean(BeaconControlProvider.KEY_RUNNING, false);
        } catch (RuntimeException error) {
            return null;
        }
    }

    private void createBeaconChannel() {
        if (Build.VERSION.SDK_INT < 26) return;

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL,
                "Clock Beacon monitoring",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Persistent notification for the lock-screen clock monitor");
        channel.setShowBadge(false);
        channel.setSound(null, null);
        channel.enableVibration(false);
        manager.createNotificationChannel(channel);
    }

    private Notification buildBeaconNotice() {
        Intent openConsole = new Intent(this, BeaconConsoleActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;

        PendingIntent contentIntent =
                PendingIntent.getActivity(this, NOTIFICATION_ID, openConsole, flags);

        Notification.Builder notice = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, NOTIFICATION_CHANNEL)
                : new Notification.Builder(this);

        notice.setSmallIcon(R.drawable.ic_beacon_clock)
                .setContentTitle("Clock Beacon Lab")
                .setContentText("שעון מסך הנעילה פעיל")
                .setContentIntent(contentIntent)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setPriority(Notification.PRIORITY_LOW)
                .setSound(null)
                .setVibrate(new long[0])
                .setDefaults(0);

        Notification result = notice.build();
        result.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        return result;
    }

    private void scheduleRecovery() {
        if (recoveryPending) return;

        Boolean enabled = readEnabledState();
        if (!Boolean.TRUE.equals(enabled)) return;

        AlarmManager alarm =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent restart = recoveryIntent();
        if (alarm == null || restart == null) return;

        try {
            long when = SystemClock.elapsedRealtime() + RECOVERY_DELAY_MS;
            if (Build.VERSION.SDK_INT >= 23) {
                alarm.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, when, restart);
            } else {
                alarm.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, when, restart);
            }
            recoveryPending = true;
        } catch (RuntimeException ignored) {
        }
    }

    private void cancelRecovery() {
        cancelRecovery(this);
        recoveryPending = false;
    }

    static void cancelRecovery(Context context) {
        AlarmManager alarm =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent restart = recoveryIntent(context);
        if (alarm == null || restart == null) return;

        try {
            alarm.cancel(restart);
        } catch (RuntimeException ignored) {
        }
    }

    private PendingIntent recoveryIntent() {
        return recoveryIntent(this);
    }

    private static PendingIntent recoveryIntent(Context context) {
        Intent restart = new Intent(context, BeaconPulseService.class)
                .setAction("io.clockbeacon.minilock.command.RESTORE");

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return PendingIntent.getForegroundService(
                        context, RECOVERY_REQUEST, restart, flags);
            }
            return PendingIntent.getService(context, RECOVERY_REQUEST, restart, flags);
        } catch (RuntimeException error) {
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
