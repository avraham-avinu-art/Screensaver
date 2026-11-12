package io.clockbeacon.minilock;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class BeaconConsoleActivity extends Activity {
    private static final int REQUEST_BEACON_OVERLAY = 4207;

    private TextView stateText;
    private Button enableButton;
    private boolean waitingForOverlaySettings;
    private boolean waitingForAccessibilitySettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildConsole();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (waitingForOverlaySettings) {
            waitingForOverlaySettings = false;
            if (Settings.canDrawOverlays(this)) {
                continueEnableFlow();
            } else {
                Toast.makeText(this,
                        "לא ניתנה הרשאת הצגה מעל אפליקציות אחרות.",
                        Toast.LENGTH_LONG).show();
            }
        }

        if (waitingForAccessibilitySettings) {
            waitingForAccessibilitySettings = false;
            if (BeaconAccessibilityService.isServiceEnabled(this)) {
                turnBeaconOn();
            } else {
                Toast.makeText(this,
                        "שירות הנגישות לא הופעל. לא הופעל מעקב ברקע.",
                        Toast.LENGTH_LONG).show();
            }
        }

        refreshConsole();
    }

    private void buildConsole() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(dp(24), dp(24), dp(24), dp(24));
        page.setBackgroundColor(0xFF101010);

        TextView heading = new TextView(this);
        heading.setText("Clock Beacon Lab");
        heading.setTextColor(0xFFFFFFFF);
        heading.setTextSize(25);
        heading.setGravity(Gravity.CENTER);
        page.addView(heading, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        stateText = new TextView(this);
        stateText.setTextColor(0xFFCCCCCC);
        stateText.setTextSize(16);
        stateText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        stateParams.topMargin = dp(28);
        page.addView(stateText, stateParams);

        enableButton = new Button(this);
        enableButton.setTextSize(17);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58));
        buttonParams.topMargin = dp(24);
        page.addView(enableButton, buttonParams);
        enableButton.setOnClickListener(this::onEnablePressed);

        setContentView(page);
        refreshConsole();
    }

    private void onEnablePressed(View view) {
        boolean ready = Settings.canDrawOverlays(this)
                && BeaconAccessibilityService.isServiceEnabled(this);

        if (Boolean.TRUE.equals(readEnabled()) && ready) {
            turnBeaconOff();
        } else {
            continueEnableFlow();
        }
    }

    private void continueEnableFlow() {
        if (!Settings.canDrawOverlays(this)) {
            waitingForOverlaySettings = true;
            Intent permissionPage = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            try {
                startActivityForResult(permissionPage, REQUEST_BEACON_OVERLAY);
            } catch (RuntimeException error) {
                waitingForOverlaySettings = false;
                Toast.makeText(this,
                        "לא ניתן לפתוח את מסך ההרשאה במכשיר הזה.",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (!BeaconAccessibilityService.isServiceEnabled(this)) {
            waitingForAccessibilitySettings = true;
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (RuntimeException error) {
                waitingForAccessibilitySettings = false;
                Toast.makeText(this,
                        "לא ניתן לפתוח את הגדרות הנגישות במכשיר הזה.",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }

        turnBeaconOn();
    }

    private void turnBeaconOn() {
        if (!Settings.canDrawOverlays(this)
                || !BeaconAccessibilityService.isServiceEnabled(this)) {
            refreshConsole();
            return;
        }

        if (!storeEnabled(true)) {
            Toast.makeText(this, "לא ניתן לשמור את בחירת ההפעלה.", Toast.LENGTH_LONG).show();
            refreshConsole();
            return;
        }

        Intent service = new Intent(this, BeaconPulseService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service);
            } else {
                startService(service);
            }
            Toast.makeText(this, "המעקב הופעל.", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException error) {
            storeEnabled(false);
            Toast.makeText(this, "לא ניתן להפעיל את שירות המעקב.", Toast.LENGTH_LONG).show();
        }
        refreshConsole();
    }

    private void turnBeaconOff() {
        storeEnabled(false);
        stopService(new Intent(this, BeaconPulseService.class));
        Toast.makeText(this, "המעקב כובה.", Toast.LENGTH_SHORT).show();
        refreshConsole();
    }

    private void refreshConsole() {
        if (stateText == null || enableButton == null) return;

        boolean enabled = Boolean.TRUE.equals(readEnabled());
        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean accessibilityEnabled =
                BeaconAccessibilityService.isServiceEnabled(this);

        if (enabled && overlayGranted && accessibilityEnabled) {
            stateText.setText("שירות המעקב פעיל.\nהשעון יופיע כשהמסך יתעורר והמכשיר יהיה נעול.");
            enableButton.setText("כבה את המעקב");
        } else if (!overlayGranted) {
            stateText.setText("יש לאשר הצגה מעל אפליקציות אחרות כדי להציג את השעון במסך הנעילה.");
            enableButton.setText("אישור הצגה מעל אפליקציות אחרות");
        } else if (!accessibilityEnabled) {
            stateText.setText("כדי לשמור את מנגנון השעון פעיל לאחר סגירת מסך האפליקציה, יש להפעיל את שירות Clock Beacon בהגדרות הנגישות.");
            enableButton.setText("הפעל שירות נגישות");
        } else {
            stateText.setText("שתי ההרשאות קיימות. אפשר להפעיל את שירות המעקב.");
            enableButton.setText("הפעל מעקב ושעון במסך הנעילה");
        }
    }

    private Boolean readEnabled() {
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

    private boolean storeEnabled(boolean enabled) {
        try {
            Bundle values = new Bundle();
            values.putBoolean(BeaconControlProvider.KEY_RUNNING, enabled);
            Bundle result = getContentResolver().call(
                    Uri.parse("content://" + BeaconControlProvider.AUTHORITY),
                    BeaconControlProvider.METHOD_STORE, null, values);
            return result != null && result.getBoolean("stored", false);
        } catch (RuntimeException error) {
            return false;
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
