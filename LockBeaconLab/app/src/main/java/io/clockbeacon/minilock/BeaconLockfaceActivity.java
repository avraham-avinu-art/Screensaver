package io.clockbeacon.minilock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextClock;

public final class BeaconLockfaceActivity extends Activity {
    private boolean closeSignalRegistered;

    private final BroadcastReceiver closeSignal = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(false);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setStatusBarColor(Color.BLACK);

        FrameLayout surface = new FrameLayout(this);
        surface.setBackgroundColor(Color.BLACK);

        TextClock clock = new TextClock(this);
        clock.setTextColor(Color.WHITE);
        clock.setGravity(Gravity.CENTER);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 72);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("h:mm");
        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        surface.addView(clock, clockParams);

        setContentView(surface);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerCloseSignal();
    }

    @Override
    protected void onStop() {
        unregisterCloseSignal();
        super.onStop();
    }

    private void registerCloseSignal() {
        if (closeSignalRegistered) return;

        IntentFilter signals = new IntentFilter();
        signals.addAction(BeaconPulseService.ACTION_HIDE_CLOCK);

        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(closeSignal, signals, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(closeSignal, signals);
            }
            closeSignalRegistered = true;
        } catch (RuntimeException ignored) {
            closeSignalRegistered = false;
        }
    }

    private void unregisterCloseSignal() {
        if (!closeSignalRegistered) return;
        try {
            unregisterReceiver(closeSignal);
        } catch (RuntimeException ignored) {
        }
        closeSignalRegistered = false;
    }
}
