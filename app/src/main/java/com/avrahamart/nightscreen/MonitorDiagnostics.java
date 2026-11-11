package com.avrahamart.nightscreen;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

/** Small private on-device event log for diagnosing the foreground monitor. */
final class MonitorDiagnostics {
    private static final String TAG = "ScreensaverMonitor";
    private static final String FILE_NAME = "monitor_diagnostics.log";
    private static final int MAX_BYTES = 128 * 1024;
    private static final int MONITOR_NOTIFICATION_ID = AutoStartNotificationController.NOTIFICATION_ID;
    private static final String MONITOR_CHANNEL_ID = AutoStartNotificationController.CHANNEL_ID;
    private static final String ACTION_TASK_REMOVED_PROBE =
            "com.avrahamart.nightscreen.action.TASK_REMOVED_DIAGNOSTIC_PROBE";
    private static final int TASK_REMOVED_PROBE_REQUEST_BASE = 6730;
    private static final int TEST_WINDOW_PROBE_REQUEST_BASE = 6800;

    private MonitorDiagnostics() { }

    static synchronized void record(Context context, String message) {
        if (context == null || message == null) return;
        String timestamp = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
        String line = timestamp + " | pid=" + android.os.Process.myPid()
                + " | " + message + "\n";
        Log.i(TAG, message);
        try {
            File file = new File(context.getFilesDir(), FILE_NAME);
            if (file.exists() && file.length() > MAX_BYTES) {
                File backup = new File(context.getFilesDir(), FILE_NAME + ".old");
                if (backup.exists()) backup.delete();
                if (!file.renameTo(backup)) file.delete();
            }
            try (FileOutputStream output = new FileOutputStream(file, true)) {
                output.write(line.getBytes(StandardCharsets.UTF_8));
                output.flush();
            }
        } catch (Throwable error) {
            Log.e(TAG, "Unable to persist diagnostic event", error);
        }
    }

    /**
     * Schedules independent checkpoints after the task is swiped away. These alarms
     * target a separate receiver in the app process, so they can still record the
     * notification state if the :lockmonitor process has been killed.
     */
    static void scheduleTaskRemovedProbes(Context context) {
        if (context == null) return;
        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            record(context, "task-removal diagnostic probes not scheduled: AlarmManager unavailable");
            return;
        }
        long scheduledAt = SystemClock.elapsedRealtime();
        long[] delays = new long[]{1_000L, 4_000L, 8_000L};
        for (int i = 0; i < delays.length; i++) {
            long delay = delays[i];
            String checkpoint = "after task removal +" + delay + "ms";
            Intent intent = new Intent(context, MonitorDiagnosticsProbeReceiver.class);
            intent.setAction(ACTION_TASK_REMOVED_PROBE);
            intent.putExtra("checkpoint", checkpoint);
            intent.putExtra("scheduledAtElapsedRealtime", scheduledAt);
            intent.putExtra("delayMs", delay);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            try {
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context, TASK_REMOVED_PROBE_REQUEST_BASE + i, intent, flags);
                long triggerAt = scheduledAt + delay;
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt, pendingIntent);
                record(context, "task-removal diagnostic probe scheduled; checkpoint="
                        + checkpoint + ", triggerElapsedRealtime=" + triggerAt);
            } catch (Throwable error) {
                record(context, "task-removal diagnostic probe scheduling failed; checkpoint="
                        + checkpoint + ", error=" + error);
            }
        }
    }

    /**
     * Schedules a bounded series of snapshots from the moment the user starts
     * a diagnostic test. Unlike onTaskRemoved probes, these do not depend on
     * the service callback being delivered promptly.
     */
    static void scheduleTestWindowProbes(Context context) {
        if (context == null) return;
        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            record(context, "test-window probes not scheduled: AlarmManager unavailable");
            return;
        }
        long scheduledAt = SystemClock.elapsedRealtime();
        long[] delays = new long[]{
                5_000L, 10_000L, 15_000L, 20_000L,
                30_000L, 45_000L, 60_000L, 90_000L, 120_000L
        };
        for (int i = 0; i < delays.length; i++) {
            long delay = delays[i];
            String checkpoint = "test-window +" + (delay / 1000L) + "s";
            Intent intent = new Intent(context, MonitorDiagnosticsProbeReceiver.class);
            intent.setAction(ACTION_TASK_REMOVED_PROBE);
            intent.putExtra("checkpoint", checkpoint);
            intent.putExtra("scheduledAtElapsedRealtime", scheduledAt);
            intent.putExtra("delayMs", delay);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            try {
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context, TEST_WINDOW_PROBE_REQUEST_BASE + i, intent, flags);
                long triggerAt = scheduledAt + delay;
                if (Build.VERSION.SDK_INT >= 23) {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
                }
                record(context, "test-window probe scheduled; checkpoint=" + checkpoint
                        + ", triggerElapsedRealtime=" + triggerAt);
            } catch (Throwable error) {
                record(context, "test-window probe scheduling failed; checkpoint="
                        + checkpoint + ", error=" + error);
            }
        }
    }

    /** Records Android's observable notification state, not merely a successful startForeground call. */
    static void recordNotificationState(Context context, String checkpoint) {
        if (context == null) return;
        boolean appNotificationsEnabled = false;
        boolean channelExists = false;
        int channelImportance = -1;
        boolean notificationPresent = false;
        boolean notificationListenerAccess = NightScreenApplication.hasNotificationAccess(context);
        String notificationDetails = "unavailable";
        try {
            NotificationManager manager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                appNotificationsEnabled = manager.areNotificationsEnabled();
                if (Build.VERSION.SDK_INT >= 26) {
                    android.app.NotificationChannel channel =
                            manager.getNotificationChannel(MONITOR_CHANNEL_ID);
                    channelExists = channel != null;
                    if (channel != null) channelImportance = channel.getImportance();
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    for (StatusBarNotificationInfo item : getPostedNotifications(manager)) {
                        if (item.id == MONITOR_NOTIFICATION_ID) {
                            notificationPresent = true;
                            notificationDetails = "tag=" + item.tag + ", ongoing=" + item.ongoing
                                    + ", flags=" + item.flags;
                            break;
                        }
                    }
                }
            }
        } catch (Throwable error) {
            notificationDetails = "queryError=" + error.getClass().getSimpleName()
                    + ":" + error.getMessage();
        }

        String processState = "not-listed";
        try {
            ActivityManager am = (ActivityManager)
                    context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> processes =
                        am.getRunningAppProcesses();
                if (processes == null) {
                    processState = "process-list-null";
                } else {
                    for (ActivityManager.RunningAppProcessInfo process : processes) {
                        if ((context.getPackageName() + ":lockmonitor").equals(process.processName)) {
                            processState = "running; pid=" + process.pid
                                    + "; importance=" + process.importance;
                            break;
                        }
                    }
                }
            }
        } catch (Throwable error) {
            processState = "queryError=" + error.getClass().getSimpleName();
        }

        record(context, "notification snapshot [" + checkpoint + "]; appNotificationsEnabled="
                + appNotificationsEnabled + ", channelExists=" + channelExists
                + ", channelImportance=" + channelImportance
                + ", notificationPresent=" + notificationPresent
                + ", notificationListenerAccess=" + notificationListenerAccess
                + ", notificationDetails={" + notificationDetails + "}"
                + ", lockmonitorProcess={" + processState + "}");
    }

    private static List<StatusBarNotificationInfo> getPostedNotifications(
            NotificationManager manager) {
        java.util.ArrayList<StatusBarNotificationInfo> result = new java.util.ArrayList<>();
        if (Build.VERSION.SDK_INT >= 23) {
            for (android.service.notification.StatusBarNotification item
                    : manager.getActiveNotifications()) {
                result.add(new StatusBarNotificationInfo(item.getId(), item.getTag(),
                        item.getNotification().flags));
            }
        }
        return result;
    }

    private static final class StatusBarNotificationInfo {
        final int id;
        final String tag;
        final int flags;
        final boolean ongoing;
        StatusBarNotificationInfo(int id, String tag, int flags) {
            this.id = id;
            this.tag = tag;
            this.flags = flags;
            this.ongoing = (flags & Notification.FLAG_ONGOING_EVENT) != 0;
        }
    }

    static synchronized String read(Context context) {
        try {
            File file = new File(context.getFilesDir(), FILE_NAME);
            if (!file.exists()) return "";
            try (FileInputStream input = new FileInputStream(file);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } catch (Throwable error) {
            Log.e(TAG, "Unable to read diagnostic log", error);
            return "לא ניתן לקרוא את נתוני האבחון: " + error;
        }
    }

    static synchronized void clear(Context context) {
        try {
            File file = new File(context.getFilesDir(), FILE_NAME);
            if (file.exists() && !file.delete()) {
                try (FileOutputStream output = new FileOutputStream(file, false)) {
                    output.write(new byte[0]);
                }
            }
            File backup = new File(context.getFilesDir(), FILE_NAME + ".old");
            if (backup.exists()) backup.delete();
        } catch (Throwable error) {
            Log.e(TAG, "Unable to clear diagnostic log", error);
        }
    }
}
