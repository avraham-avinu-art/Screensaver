package io.clockbeacon.minilock;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextClock;

public final class BeaconPulseService extends Service {
    static final String ACTION_HIDE_CLOCK =
            "io.clockbeacon.minilock.event.HIDE_CLOCKFACE";

    private static final String NOTIFICATION_CHANNEL = "clock_beacon_engine";
    private static final int NOTIFICATION_ID = 8417;

    private static final long[] RETRY_DELAYS = {
            1_000L, 2_000L, 5_000L, 10_000L, 30_000L
    };

    private final Handler engineHandler = new Handler(Looper.getMainLooper());

    private BeaconScreenEventReceiver screenEvents;
    private boolean screenEventsRegistered;
    private boolean engineForeground;
    private boolean enabledConfirmed;
    private boolean retryPending;
    private boolean destroying;
    private int retryIndex;
    private int wakeSequence;

    private WindowManager windowManager;
    private View clockOverlay;

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
        promoteBeaconForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!engineForeground && !promoteBeaconForeground()) {
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        cancelStateRetry();

        Boolean enabled = readEnabledState();
        if (enabled == null) {
            scheduleStateRetry();
            return START_STICKY;
        }

        if (!enabled || !Settings.canDrawOverlays(this)) {
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
        PowerManager power =
                (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (power != null && power.isInteractive()) {
            onDisplayAwake();
        }

        return START_STICKY;
    }

    private boolean promoteBeaconForeground() {
        try {
            Notification notice = buildBeaconNotice();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

        if (!enabled || !Settings.canDrawOverlays(this)) {
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
        IntentFilter signals = new IntentFilter();
        signals.addAction(Intent.ACTION_SCREEN_ON);
        signals.addAction(Intent.ACTION_SCREEN_OFF);
        signals.addAction(Intent.ACTION_USER_PRESENT);

        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(candidate, signals, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(candidate, signals);
            }
            screenEvents = candidate;
            screenEventsRegistered = true;
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    void onDisplayAwake() {
        int sequence = ++wakeSequence;
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
            showClockOverlay();
            return;
        }

        if (attempt < 5) {
            engineHandler.postDelayed(
                    () -> attemptClockface(sequence, attempt + 1), 300L);
        } else {
            hideClockface();
        }
    }

    private void showClockOverlay() {
        if (!Settings.canDrawOverlays(this)) return;
        if (clockOverlay != null && clockOverlay.getParent() != null) return;

        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        }
        if (windowManager == null) return;

        FrameLayout surface = new FrameLayout(this);
        surface.setBackgroundColor(Color.BLACK);

        TextClock clock = new TextClock(this);
        clock.setTextColor(Color.WHITE);
        clock.setGravity(Gravity.CENTER);
        clock.setTextSize(72);
        clock.setIncludeFontPadding(false);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("h:mm");

        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        surface.addView(clock, clockParams);

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.OPAQUE);
        params.gravity = Gravity.CENTER;
        params.setTitle("Clock Beacon lock-screen clock");

        try {
            windowManager.addView(surface, params);
            clockOverlay = surface;
        } catch (RuntimeException error) {
            clockOverlay = null;
        }
    }

    void hideClockface() {
        wakeSequence++;
        if (clockOverlay == null) return;

        try {
            if (windowManager != null && clockOverlay.getParent() != null) {
                windowManager.removeViewImmediate(clockOverlay);
            }
        } catch (RuntimeException ignored) {
        } finally {
            clockOverlay = null;
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        destroying = true;
        cancelStateRetry();
        boolean shouldRestart = shouldRecoverAfterUnexpectedStop();

        hideClockface();
        unregisterScreenEvents();
        if (engineForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            engineForeground = false;
        }

        super.onDestroy();

        if (shouldRestart) {
            sendBroadcast(new Intent(this, BeaconRestartReceiver.class));
        }
    }

    private boolean shouldRecoverAfterUnexpectedStop() {
        return Boolean.TRUE.equals(readEnabledState())
                && Settings.canDrawOverlays(this)
                && BeaconAccessibilityService.isServiceEnabled(this);
    }

    private void shutDownEngine() {
        cancelStateRetry();
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
        channel.setDescription(
                "Persistent notification for the lock-screen clock monitor");
        channel.setShowBadge(false);
        channel.setSound(null, null);
        channel.enableVibration(false);
        manager.createNotificationChannel(channel);
    }

    private Notification buildBeaconNotice() {
        Intent openConsole = new Intent(this, BeaconConsoleActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent contentIntent =
                PendingIntent.getActivity(this, NOTIFICATION_ID, openConsole, flags);

        Notification.Builder notice =
                new Notification.Builder(this, NOTIFICATION_CHANNEL);

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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
