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
    private static final String KEY_ACTIVE_COMPONENT = "screensaver_active_component";
    private static final String KEY_ACTIVATE_ON_SLEEP = "screensaver_activate_on_sleep";

    private AndroidDreamSettings() { }

    static boolean isReadyForCharging(Context context) {
        try {
            if (Settings.Secure.getInt(
                    context.getContentResolver(), KEY_ENABLED, 0) != 1) return false;
            ComponentName expected =
                    new ComponentName(context, ScreensaverDreamService.class);
            String expectedFlat = expected.flattenToString();
            String components = Settings.Secure.getString(
                    context.getContentResolver(), KEY_COMPONENTS);
            String legacy = Settings.Secure.getString(
                    context.getContentResolver(), KEY_COMPONENT);
            String defaultComponent = Settings.Secure.getString(
                    context.getContentResolver(), KEY_DEFAULT_COMPONENT);
            String activeComponent = Settings.Secure.getString(
                    context.getContentResolver(), KEY_ACTIVE_COMPONENT);
            boolean hasComponentList = components != null && !components.trim().isEmpty();
            boolean selected = hasComponentList
                    ? contains(components, expected, expectedFlat)
                    : contains(legacy, expected, expectedFlat)
                    || contains(defaultComponent, expected, expectedFlat)
                    || contains(activeComponent, expected, expectedFlat);
            if (!selected) return false;
            // On releases that expose the system's resolved active component,
            // it must also identify this Dream service.
            if (activeComponent != null && !activeComponent.trim().isEmpty()
                    && !contains(activeComponent, expected, expectedFlat)) return false;
            // Charging activation is SCREENSAVER_ACTIVATE_ON_SLEEP. Dock-only
            // activation is not equivalent to ordinary charging.
            return Settings.Secure.getInt(context.getContentResolver(),
                    KEY_ACTIVATE_ON_SLEEP, 0) == 1;
        } catch (Throwable ignored) {
            return false;
        }
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
