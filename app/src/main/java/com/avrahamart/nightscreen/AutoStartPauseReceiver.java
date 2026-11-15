package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** Changes only the global temporary pause state; all individual settings remain untouched. */
public final class AutoStartPauseReceiver extends BroadcastReceiver {
    static final String ACTION_PAUSE =
            "com.avrahamart.nightscreen.action.PAUSE_AUTOMATIC_START";
    static final String ACTION_RESUME =
            "com.avrahamart.nightscreen.action.RESUME_AUTOMATIC_START";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        final boolean paused;
        if (ACTION_PAUSE.equals(intent.getAction())) {
            paused = true;
        } else if (ACTION_RESUME.equals(intent.getAction())) {
            paused = false;
        } else {
            return;
        }

        SharedPreferences prefs =
                context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean saved = prefs.edit()
                .putBoolean("autoStartPaused", paused)
                .commit();
        if (!saved) {
            android.util.Log.e("NightScreenAutoStart",
                    "Unable to save the temporary automatic-start pause state.");
            return;
        }

        // A resumed monitor begins with a fresh lock cycle. No stored user
        // activation settings are changed by either action.
        MonitorSettingsSnapshot.resetDeviceLockActivation(context);
        MonitorSettingsSnapshot.setSuppressNextDeviceLockScreenOff(context, false);
        NightScreenApplication.syncChargerReceiver(context);
        LockScreenMonitorService.sync(context);
    }
}
