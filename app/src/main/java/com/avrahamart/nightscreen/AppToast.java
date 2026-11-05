package com.avrahamart.nightscreen;

import android.content.Context;
import android.widget.Toast;

/**
 * Replaces any still-visible in-app toast immediately before showing a new one.
 * Trailing sentence periods are omitted consistently across the application.
 */
public final class AppToast {
    private static Toast activeToast;

    private AppToast() {}

    public static synchronized Toast makeText(Context context,
                                               CharSequence message,
                                               int duration) {
        if (activeToast != null) {
            activeToast.cancel();
            activeToast = null;
        }
        String text = message == null ? "" : message.toString().trim();
        text = text.replaceAll("[.。]+$", "");
        activeToast = Toast.makeText(context.getApplicationContext(), text, duration);
        return activeToast;
    }
}