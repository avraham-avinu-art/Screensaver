package com.avrahamart.nightscreen;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Small private, on-device event log for diagnosing the foreground monitor. */
final class MonitorDiagnostics {
    private static final String TAG = "ScreensaverMonitor";
    private static final String FILE_NAME = "monitor_diagnostics.log";
    private static final int MAX_BYTES = 128 * 1024;

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
