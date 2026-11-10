package com.avrahamart.nightscreen;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Regression tests for independent activation sources and lock-screen policy. */
public class AutoStartPolicyTest {
    @Test
    public void lockPreferenceEnablesOnlyScreenOff() {
        assertTrue(enabled(AutoStartPolicy.ACTION_SCREEN_OFF, true, false, false, false, false));
        assertFalse(enabled(AutoStartPolicy.ACTION_BOOT_COMPLETED, true, false, false, false, false));
        assertFalse(enabled(AutoStartPolicy.ACTION_SCREEN_ON, true, false, false, false, false));
        assertFalse(enabled(AutoStartPolicy.ACTION_POWER_CONNECTED, true, false, false, false, false));
    }

    @Test
    public void bootPreferenceEnablesOnlyBootCompleted() {
        assertFalse(enabled(AutoStartPolicy.ACTION_SCREEN_OFF, false, true, false, false, false));
        assertTrue(enabled(AutoStartPolicy.ACTION_BOOT_COMPLETED, false, true, false, false, false));
        assertFalse(enabled(AutoStartPolicy.ACTION_SCREEN_ON, false, true, false, false, false));
        assertFalse(enabled(AutoStartPolicy.ACTION_POWER_CONNECTED, false, true, false, false, false));
    }

    @Test
    public void wakePreferenceEnablesOnlyScreenOn() {
        assertFalse(enabled(AutoStartPolicy.ACTION_SCREEN_OFF, false, false, true, false, false));
        assertFalse(enabled(AutoStartPolicy.ACTION_BOOT_COMPLETED, false, false, true, false, false));
        assertTrue(enabled(AutoStartPolicy.ACTION_SCREEN_ON, false, false, true, false, false));
        assertFalse(enabled(AutoStartPolicy.ACTION_POWER_CONNECTED, false, false, true, false, false));
    }

    @Test
    public void chargePreferenceEnablesOnlyPowerConnected() {
        assertFalse(enabled(AutoStartPolicy.ACTION_SCREEN_OFF, false, false, false, true, false));
        assertFalse(enabled(AutoStartPolicy.ACTION_BOOT_COMPLETED, false, false, false, true, false));
        assertFalse(enabled(AutoStartPolicy.ACTION_SCREEN_ON, false, false, false, true, false));
        assertTrue(enabled(AutoStartPolicy.ACTION_POWER_CONNECTED, false, false, false, true, false));
        assertFalse(enabled(AutoStartPolicy.ACTION_POWER_CONNECTED, false, false, false, true, true));
    }

    @Test
    public void unrelatedEnabledSettingsNeverEnableAnotherEvent() {
        assertFalse(enabled(AutoStartPolicy.ACTION_SCREEN_OFF, false, true, true, true, false));
        assertFalse(enabled(AutoStartPolicy.ACTION_BOOT_COMPLETED, true, false, true, true, false));
        assertFalse(enabled(AutoStartPolicy.ACTION_SCREEN_ON, true, true, false, true, false));
        assertFalse(enabled(AutoStartPolicy.ACTION_POWER_CONNECTED, true, true, true, false, false));
    }

    @Test
    public void unknownActionCannotStartScreensaver() {
        assertNull(AutoStartPolicy.triggerForAction("android.intent.action.UNKNOWN"));
        assertFalse(enabled("android.intent.action.UNKNOWN", true, true, true, true, false));
    }

    @Test
    public void lockAndWakeDoNotRecreateAnExistingScreensaverSession() {
        assertFalse(AutoStartPolicy.shouldLaunchNewSession(true, true));
        assertTrue(AutoStartPolicy.shouldLaunchNewSession(true, false));
        assertFalse(AutoStartPolicy.shouldLaunchNewSession(false, false));
    }

    @Test
    public void newAutomaticStartsAreIndependentOfPostTimeoutContinuation() {
        // The continuation switch is deliberately not part of the new-session gate.
        assertTrue(AutoStartPolicy.shouldLaunchNewSession(true, false));
    }

    @Test
    public void waitingContinuationIsNotAnActiveActivitySession() {
        assertTrue(AutoStartPolicy.isExistingActivitySession(true, false, true));
        assertFalse(AutoStartPolicy.isExistingActivitySession(true, true, true));
        assertFalse(AutoStartPolicy.isExistingActivitySession(true, false, false));
        assertFalse(AutoStartPolicy.isExistingActivitySession(false, false, true));
    }

    @Test
    public void activeScreensaverShowsAboveKeyguardWithoutContinuationSwitch() {
        assertTrue(AutoStartPolicy.shouldShowWhenLocked(true, false, false));
    }

    @Test
    public void postTimeoutContinuationRequiresSwitchAndArmedState() {
        assertTrue(AutoStartPolicy.shouldShowWhenLocked(false, true, true));
        assertFalse(AutoStartPolicy.shouldShowWhenLocked(false, true, false));
        assertFalse(AutoStartPolicy.shouldShowWhenLocked(false, false, true));
    }

    @Test
    public void anyOneAutostartModeKeepsThePersistentNotificationService() {
        assertFalse(AutoStartPolicy.needsMonitorService(false, false, false, false));
        assertTrue(AutoStartPolicy.needsMonitorService(true, false, false, false));
        assertTrue(AutoStartPolicy.needsMonitorService(false, true, false, false));
        assertTrue(AutoStartPolicy.needsMonitorService(false, false, true, false));
        assertTrue(AutoStartPolicy.needsMonitorService(false, false, false, true));
    }

    private boolean enabled(String action, boolean lock, boolean boot,
                            boolean wake, boolean charge, boolean dream) {
        return AutoStartPolicy.isEnabledForAction(
                action, lock, boot, wake, charge, dream);
    }
}
