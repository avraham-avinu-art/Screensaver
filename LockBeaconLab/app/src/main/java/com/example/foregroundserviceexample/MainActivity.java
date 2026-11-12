package com.example.foregroundserviceexample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextClock;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public final class MainActivity extends Activity {
    private boolean servicesStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        TextClock clock = new TextClock(this);
        clock.setTextColor(0xFFFFFFFF);
        clock.setGravity(Gravity.CENTER);
        clock.setTextSize(72);
        clock.setIncludeFontPadding(false);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("h:mm");

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        root.addView(clock, params);
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Settings.canDrawOverlays(this)) {
            try {
                Intent permissionPage = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(permissionPage);
            } catch (RuntimeException error) {
                Toast.makeText(this, "לא ניתן לפתוח את הרשאת ההצגה מעל אפליקציות.", Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (!servicesStarted) {
            servicesStarted = true;

            Intent foreground = new Intent(this, CounterService.class);
            foreground.setAction("START");
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(this, foreground);
                } else {
                    startService(foreground);
                }

                startService(new Intent(this, ClockOverlayService.class));
            } catch (RuntimeException error) {
                servicesStarted = false;
                Toast.makeText(this, "לא ניתן להפעיל את שירות השעון.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
