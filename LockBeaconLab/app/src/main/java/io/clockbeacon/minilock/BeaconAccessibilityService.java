package io.clockbeacon.minilock;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;

public final class BeaconAccessibilityService extends AccessibilityService {

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        if (!Boolean.TRUE.equals(readEnabledState(this))) return;
        if (!Settings.canDrawOverlays(this)) return;

        Intent engine = new Intent(this, BeaconPulseService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(engine);
            } else {
                startService(engine);
            }
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopService(new Intent(this, BeaconPulseService.class));
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // This service does not inspect the event or the active window.
    }

    @Override
    public void onInterrupt() {
    }

    static boolean isServiceEnabled(Context context) {
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices == null || enabledServices.isEmpty()) return false;

        String expected = new ComponentName(
                context, BeaconAccessibilityService.class).flattenToString();

        for (String entry : enabledServices.split(":")) {
            if (expected.equalsIgnoreCase(entry)) return true;
        }
        return false;
    }

    private static Boolean readEnabledState(Context context) {
        try {
            Bundle state = context.getContentResolver().call(
                    Uri.parse("content://" + BeaconControlProvider.AUTHORITY),
                    BeaconControlProvider.METHOD_FETCH, null, null);
            return state == null ? null
                    : state.getBoolean(BeaconControlProvider.KEY_RUNNING, false);
        } catch (RuntimeException error) {
            return null;
        }
    }
}
