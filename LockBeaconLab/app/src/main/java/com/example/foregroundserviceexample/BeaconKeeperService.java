package com.example.foregroundserviceexample;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextClock;

import androidx.core.app.NotificationCompat;

public final class BeaconKeeperService extends Service {
    private static final String CHANNEL_ID = "clock_beacon_keeper";
    private static final int NOTIFICATION_ID = 41;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private FrameLayout clockWindow;
    private boolean receiverRegistered;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                handler.postDelayed(BeaconKeeperService.this::showClockIfLocked, 300L);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)
                    || Intent.ACTION_USER_PRESENT.equals(action)) {
                hideClock();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        registerScreenReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Promote immediately on every start, including a START_STICKY null-intent restart.
        startForeground(NOTIFICATION_ID, buildNotification());

        handler.postDelayed(this::showClockIfLocked, 300L);
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Clock Beacon service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the clock display service active");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 41, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_android)
                .setContentTitle("Clock Beacon פעיל")
                .setContentText("שירות השעון פועל ברקע")
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void registerScreenReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(screenReceiver, filter);
            }
            receiverRegistered = true;
        } catch (RuntimeException ignored) {
            receiverRegistered = false;
        }
    }

    private void showClockIfLocked() {
        PowerManager power = getSystemService(PowerManager.class);
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        if (power == null || keyguard == null || !power.isInteractive()) {
            hideClock();
            return;
        }
        if (keyguard.isKeyguardLocked()) showClock();
        else hideClock();
    }

    private void showClock() {
        if (!Settings.canDrawOverlays(this)) return;
        if (clockWindow != null && clockWindow.getParent() != null) return;

        if (windowManager == null) {
            windowManager = getSystemService(WindowManager.class);
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

        int windowFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                windowFlags,
                PixelFormat.OPAQUE);
        params.gravity = Gravity.CENTER;
        params.setTitle("Clock Beacon lock-screen clock");

        try {
            windowManager.addView(surface, params);
            clockWindow = surface;
        } catch (RuntimeException ignored) {
            clockWindow = null;
        }
    }

    private void hideClock() {
        if (clockWindow == null) return;
        try {
            if (windowManager != null && clockWindow.getParent() != null) {
                windowManager.removeViewImmediate(clockWindow);
            }
        } catch (RuntimeException ignored) {
        } finally {
            clockWindow = null;
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // android:stopWithTask=false keeps this started service alive when its task is removed.
        // Do not tie service lifetime to the launcher Activity or the task stack.
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        hideClock();
        if (receiverRegistered) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (RuntimeException ignored) {
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
