package com.avrahamart.nightscreen;

import android.content.Intent;
import android.service.dreams.DreamService;

/**
 * Android Dream entry point for charging activation.
 *
 * The Dream is intentionally a trampoline only: the complete, ordinary
 * MainActivity is the screensaver UI for both charging activation routes.
 */
public final class ScreensaverDreamService extends DreamService {
    private boolean launchRequested;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        launchFullScreensaver();
    }

    private void launchFullScreensaver() {
        if (launchRequested) return;
        launchRequested = true;

        android.content.SharedPreferences prefs =
                getSharedPreferences("settings", MODE_PRIVATE);
        if (!prefs.getBoolean("autoStartOnCharge", false)
                || !prefs.getBoolean("useAndroidDreamForCharging", false)) {
            finish();
            return;
        }

        Intent launch = new Intent(this, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(MainActivity.EXTRA_OPENED_BY_CHARGER, true);
        try {
            startActivity(launch);
        } catch (Throwable ignored) {
            // If Android blocks the foreground transition, leave the Dream intact
            // rather than replacing it with a visually incomplete fallback.
        }
        finish();
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
    }
}
