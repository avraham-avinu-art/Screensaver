package com.avrahamart.nightscreen;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores the lock policy using its current positive meaning:
 * true means prevent the keyguard from appearing over an active screensaver.
 *
 * The previous "showOnLockScreen" preference had the opposite meaning.
 * Its explicit value is inverted once during migration; an unset old preference
 * receives the new default (prevention disabled).
 */
public final class LockBehaviorPreferences {
    private static final String PREFS_NAME = "settings";
    private static final String KEY_PREVENT = "preventLockWhileActive";
    private static final String LEGACY_KEY_SHOW_ON_LOCK = "showOnLockScreen";
    private static final String KEY_DEFAULT_OFF_MIGRATION =
            "preventLockWhileActiveDefaultOffMigrationV1";

    private LockBehaviorPreferences() {
    }

    public static boolean isPreventionEnabled(Context context) {
        SharedPreferences prefs =
                context.getApplicationContext().getSharedPreferences(
                        PREFS_NAME, Context.MODE_PRIVATE);

        // Build 424 accidentally persisted "true" as the default. On the first
        // launch of this correction, reset that implicit value to the user's
        // intended default (off). There is no old-key copy in 424 to distinguish
        // that default from a manually selected value, so a true value from that
        // one release is reset once; the user can enable it again explicitly.
        if (!prefs.getBoolean(KEY_DEFAULT_OFF_MIGRATION, false)) {
            boolean value = false;
            if (prefs.contains(LEGACY_KEY_SHOW_ON_LOCK)) {
                Object oldValue = prefs.getAll().get(LEGACY_KEY_SHOW_ON_LOCK);
                boolean oldShowOnLock = oldValue instanceof Boolean
                        ? (Boolean) oldValue
                        : oldValue instanceof String
                                ? Boolean.parseBoolean((String) oldValue)
                                : oldValue instanceof Number
                                        ? ((Number) oldValue).intValue() != 0
                                        : true;
                value = !oldShowOnLock;
            }
            prefs.edit().putBoolean(KEY_PREVENT, value)
                    .putBoolean(KEY_DEFAULT_OFF_MIGRATION, true)
                    .remove(LEGACY_KEY_SHOW_ON_LOCK).commit();
        } else if (prefs.contains(LEGACY_KEY_SHOW_ON_LOCK)) {
            prefs.edit().remove(LEGACY_KEY_SHOW_ON_LOCK).apply();
        }
        return prefs.getBoolean(KEY_PREVENT, false);
    }}
