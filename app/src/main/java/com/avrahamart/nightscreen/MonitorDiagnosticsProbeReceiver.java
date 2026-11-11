package com.avrahamart.nightscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Captures delayed notification/process snapshots after the user removes the
 * launcher task. This receiver runs in the default app process, independently
 * of the dedicated :lockmonitor service process.
 */
public final class MonitorDiagnosticsProbeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String checkpoint = intent == null
                ? "unknown checkpoint"
                : intent.getStringExtra("checkpoint");
        long scheduledAt = intent == null
                ? -1L : intent.getLongExtra("scheduledAtElapsedRealtime", -1L);
        long delayMs = intent == null
                ? -1L : intent.getLongExtra("delayMs", -1L);
        long elapsed = SystemClock.elapsedRealtime();
        long actualSinceSchedule = scheduledAt >= 0L ? elapsed - scheduledAt : -1L;

        MonitorDiagnostics.record(context,
                "task-removal diagnostic probe RECEIVED; checkpoint=" + checkpoint
                        + ", requestedDelayMs=" + delayMs
                        + ", actualSinceScheduleMs=" + actualSinceSchedule
                        + ", receiverPid=" + android.os.Process.myPid());
        MonitorDiagnostics.recordNotificationState(context, checkpoint);
    }
}
