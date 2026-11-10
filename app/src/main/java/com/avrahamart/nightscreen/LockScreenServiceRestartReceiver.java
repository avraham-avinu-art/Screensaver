package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/** Restores the foreground monitor only when the scheduled recovery alarm fires. */
public final class LockScreenServiceRestartReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreensaverMonitor";

    @Override
    public void onReceive(Context context, Intent intent) {
        MonitorDiagnostics.record(context, "restart alarm RECEIVER onReceive; action="
                + (intent == null ? "null" : intent.getAction())
                + ", elapsedRealtime=" + android.os.SystemClock.elapsedRealtime());
        MonitorDiagnostics.recordNotificationState(context, "alarm receiver before service start");
        Intent serviceIntent = new Intent(context, LockScreenMonitorService.class);
        serviceIntent.setAction(LockScreenMonitorService.ACTION_ALARM_RESTART);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            MonitorDiagnostics.record(context, "alarm receiver submitted explicit service start");
        } catch (Throwable error) {
            Log.e(TAG, "Alarm receiver could not start monitor service", error);
            MonitorDiagnostics.record(context, "alarm receiver service start REJECTED: " + error);
        }
    }
}
