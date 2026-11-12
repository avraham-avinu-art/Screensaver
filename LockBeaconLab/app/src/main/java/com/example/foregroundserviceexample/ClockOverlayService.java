package com.example.foregroundserviceexample;

import android.app.KeyguardManager;
import android.app.NotificationManager;
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
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextClock;

public final class ClockOverlayService extends Service {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private WindowManager windowManager;
    private View clockWindow;
    private boolean receiverRegistered;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();

            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                handler.postDelayed(ClockOverlayService.this::showIfLocked, 250L);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)
                    || Intent.ACTION_USER_PRESENT.equals(action)) {
                hideClock();
            }
        }
    };

    private final Runnable serviceCheck = new Runnable() {
        @Override
        public void run() {
            if (!foregroundServiceNotificationExists()) {
                hideClock();
                stopSelf();
                return;
            }
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

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

        handler.post(serviceCheck);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showIfLocked();
        return START_STICKY;
    }

    private boolean foregroundServiceNotificationExists() {
        try {
            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return false;

            if (Build.VERSION.SDK_INT >= 23) {
                for (android.service.notification.StatusBarNotification entry
                        : manager.getActiveNotifications()) {
                    if (entry.getId() == 1) return true;
                }
                return false;
            }
        } catch (RuntimeException ignored) {
        }
        return true;
    }

    private void showIfLocked() {
        PowerManager power =
                (PowerManager) getSystemService(Context.POWER_SERVICE);
        KeyguardManager keyguard =
                (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        if (power == null || keyguard == null || !power.isInteractive()) return;
        if (keyguard.isKeyguardLocked()) {
            showClock();
        } else {
            hideClock();
        }
    }

    private void showClock() {
        if (!Settings.canDrawOverlays(this)) return;
        if (clockWindow != null && clockWindow.getParent() != null) return;

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
