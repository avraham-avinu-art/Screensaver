package com.avrahamart.nightscreen;

import android.content.Context;
import android.os.SystemClock;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Cross-process guard for the screen-off broadcast.
 *
 * MainActivity handles the screen-off event for a screensaver session that is
 * already on screen. The lock-monitor process receives the same broadcast and
 * must not interpret that same event as a request to start a second session.
 */
public final class ScreenOffLaunchGuard {
    private static final String FILE_NAME = "screensaver_screen_off_guard.bin";
    private static final long SUPPRESSION_WINDOW_MS = 3_000L;

    private ScreenOffLaunchGuard() {
    }

    public static void recordHandled(Context context) {
        if (context == null) return;
        File guardFile = new File(context.getNoBackupFilesDir(), FILE_NAME);
        long now = SystemClock.elapsedRealtime();
        try (RandomAccessFile file = new RandomAccessFile(guardFile, "rw");
             FileChannel channel = file.getChannel();
             FileLock ignored = channel.lock()) {
            file.setLength(0L);
            file.seek(0L);
            file.writeLong(now);
            file.getFD().sync();
        } catch (Throwable ignored) {
            // The screensaver must continue even if the guard cannot be written.
        }
    }

    public static boolean wasHandledRecently(Context context) {
        if (context == null) return false;
        File guardFile = new File(context.getNoBackupFilesDir(), FILE_NAME);
        long now = SystemClock.elapsedRealtime();
        try (RandomAccessFile file = new RandomAccessFile(guardFile, "rw");
             FileChannel channel = file.getChannel();
             FileLock ignored = channel.lock()) {
            if (file.length() < 8L) return false;
            file.seek(0L);
            long handledAt = file.readLong();
            return now >= handledAt && now - handledAt < SUPPRESSION_WINDOW_MS;
        } catch (Throwable ignored) {
            // Fail open so a genuine future device-lock event is not suppressed.
            return false;
        }
    }
}
