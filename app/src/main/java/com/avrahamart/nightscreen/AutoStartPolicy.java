package com.avrahamart.nightscreen;

/**
 * Central policy for automatic-start sources and lock-screen presentation.
 * Kept free of Android dependencies so the event matrix can be unit tested.
 */
final class AutoStartPolicy {
    static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
    static final String ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON";
    static final String ACTION_POWER_CONNECTED = "android.intent.action.ACTION_POWER_CONNECTED";
    static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    enum Trigger { DEVICE_LOCK, DEVICE_BOOT, DEVICE_WAKE, CHARGER }

    private AutoStartPolicy() {}

    static Trigger triggerForAction(String action) {
        if (ACTION_SCREEN_OFF.equals(action)) return Trigger.DEVICE_LOCK;
        if (ACTION_BOOT_COMPLETED.equals(action)) return Trigger.DEVICE_BOOT;
        if (ACTION_SCREEN_ON.equals(action)) return Trigger.DEVICE_WAKE;
        if (ACTION_POWER_CONNECTED.equals(action)) return Trigger.CHARGER;
        return null;
    }

    static boolean isEnabledForAction(
            String action,
            boolean autoStartOnDeviceLock,
            boolean autoStartOnBoot,
            boolean autoStartOnWake,
            boolean autoStartOnCharge,
            boolean useAndroidDreamForCharging) {
        Trigger trigger = triggerForAction(action);
        if (trigger == null) return false;
        switch (trigger) {
            case DEVICE_LOCK:
                return autoStartOnDeviceLock;
            case DEVICE_BOOT:
                return autoStartOnBoot;
            case DEVICE_WAKE:
                return autoStartOnWake;
            case CHARGER:
                return autoStartOnCharge && !useAndroidDreamForCharging;
            default:
                return false;
        }
    }

    static boolean needsMonitorService(
            boolean autoStartOnDeviceLock,
            boolean autoStartOnWake,
            boolean autoStartOnCharge,
            boolean useAndroidDreamForCharging) {
        // BOOT_COMPLETED has a dedicated manifest receiver. Android Dream owns
        // charging when selected, so neither needs the persistent monitor.
        return autoStartOnDeviceLock
                || autoStartOnWake
                || (autoStartOnCharge && !useAndroidDreamForCharging);
    }

    /**
     * Prevent SCREEN_OFF/SCREEN_ON triggers from recreating an already-running
     * screensaver session. The continuation preference is intentionally absent:
     * it governs only the existing session after its selected duration ends.
     */
    static boolean shouldLaunchNewSession(
            boolean triggerEnabled,
            boolean sessionAlreadyRunning) {
        return triggerEnabled && !sessionAlreadyRunning;
    }

    static boolean shouldShowWhenLocked(
            boolean activeSession,
            boolean continueOnLockScreen,
            boolean continuationArmed) {
        // Normal screensaver activity remains above Keyguard. The continuation
        // switch only controls visibility after the activity timer has ended.
        return activeSession || (continueOnLockScreen && continuationArmed);
    }
}
