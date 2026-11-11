package com.avrahamart.nightscreen;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AutoStartOnPowerReceiverTest {
    @Test
    public void wakeOnlyIgnoresAStaleConsumedLockCycle() {
        assertTrue(AutoStartOnPowerReceiver.shouldActivateAfterDeepSleep(
                true, false, true, true));
    }

    @Test
    public void activeLockModeStillPreventsDuplicateWakeActivation() {
        assertFalse(AutoStartOnPowerReceiver.shouldActivateAfterDeepSleep(
                true, true, true, true));
    }

    @Test
    public void activeLockModeAllowsWakeWhenLockCycleWasNotConsumed() {
        assertTrue(AutoStartOnPowerReceiver.shouldActivateAfterDeepSleep(
                true, true, false, true));
    }

    @Test
    public void wakeModeMustBeEnabled() {
        assertFalse(AutoStartOnPowerReceiver.shouldActivateAfterDeepSleep(
                false, false, false, true));
    }

    @Test
    public void screenMustHaveBeenInDeepSleep() {
        assertFalse(AutoStartOnPowerReceiver.shouldActivateAfterDeepSleep(
                true, false, false, false));
    }
}
