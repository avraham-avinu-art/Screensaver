package com.avrahamart.nightscreen;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

/** Reads the system-owned Dream selection and charging activation state. */
final class AndroidDreamSettings {
    private static final String KEY_ENABLED = "screensaver_enabled";
    private static final String KEY_COMPONENTS = "screensaver_components";
    private static final String KEY_COMPONENT = "screensaver_component";
    private static final String KEY_DEFAULT_COMPONENT = "screensaver_default_component";
    private static final String KEY_ACTIVATE_ON_SLEEP = "screensaver_activate_on_sleep";

    private AndroidDreamSettings() { }

    static boolean isReadyForCharging(Context context) {
        try {
            if (!readSecureBoolean(context, KEY_ENABLED,
                    "config_dreamsEnabledByDefault", false)) return false;
            ComponentName expected =
                    new ComponentName(context, ScreensaverDreamService.class);
            String expectedFlat = expected.flattenToString();
            String components = Settings.Secure.getString(
                    context.getContentResolver(), KEY_COMPONENTS);
            String legacy = Settings.Secure.getString(
                    context.getContentResolver(), KEY_COMPONENT);
            String defaultComponent = Settings.Secure.getString(
                    context.getContentResolver(), KEY_DEFAULT_COMPONENT);
            boolean hasComponentList = components != null && !components.trim().isEmpty();
            boolean selected = hasComponentList
                    ? contains(components, expected, expectedFlat)
                    : contains(legacy, expected, expectedFlat)
                    || contains(defaultComponent, expected, expectedFlat);
            if (!selected) return false;

            // Despite its legacy name, Android maps SCREENSAVER_ACTIVATE_ON_SLEEP
            // to DREAM_ON_CHARGE. The framework config is used when the secure
            // setting has not yet been explicitly persisted by Settings.
            return readSecureBoolean(context, KEY_ACTIVATE_ON_SLEEP,
                    "config_dreamsActivatedOnSleepByDefault", false);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean readSecureBoolean(Context context, String settingKey,
                                             String frameworkDefaultResource,
                                             boolean fallback) {
        try {
            String stored = Settings.Secure.getString(
                    context.getContentResolver(), settingKey);
            if (stored != null) {
                return "1".equals(stored) || Boolean.parseBoolean(stored);
            }
        } catch (Throwable ignored) {
        }

        try {
            int resourceId = context.getResources().getIdentifier(
                    frameworkDefaultResource, "bool", "android");
            if (resourceId != 0) {
                return context.getResources().getBoolean(resourceId);
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static boolean contains(String value, ComponentName expected, String flat) {
        if (value == null || value.trim().isEmpty()) return false;
        for (String token : value.split("[:,]")) {
            String candidate = token.trim();
            if (flat.equals(candidate)) return true;
            if (expected.equals(ComponentName.unflattenFromString(candidate))) return true;
        }
        return false;
    }
}
