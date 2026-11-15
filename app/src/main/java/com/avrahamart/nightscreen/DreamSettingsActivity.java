package com.avrahamart.nightscreen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Entry point used by Android's built-in Screensaver settings button. */
public final class DreamSettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        startActivity(new Intent(this, SettingsActivity.class));
        finish();
        overridePendingTransition(0, 0);
    }
}
