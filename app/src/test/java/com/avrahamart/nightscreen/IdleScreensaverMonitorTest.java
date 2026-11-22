package com.avrahamart.nightscreen;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class IdleScreensaverMonitorTest {
    @Test
    public void launchesAfterIdleEvenIfAnotherAppRemainsInFront() {
        assertTrue(IdleScreensaverMonitor.shouldLaunch(
                true, true, 60_000L, 60_000L));
    }

    @Test
    public void doesNotLaunchBeforeTheSelectedIdleInterval() {
        assertFalse(IdleScreensaverMonitor.shouldLaunch(
                true, true, 59_999L, 60_000L));
    }

    @Test
    public void doesNotLaunchWhenDisabled() {
        assertFalse(IdleScreensaverMonitor.shouldLaunch(
                false, true, 90_000L, 60_000L));
    }

    @Test
    public void doesNotLaunchWhenScreenIsNotInteractive() {
        assertFalse(IdleScreensaverMonitor.shouldLaunch(
                true, false, 90_000L, 60_000L));
    }

    @Test
    public void invalidIdleDurationNeverLaunches() {
        assertFalse(IdleScreensaverMonitor.shouldLaunch(
                true, true, 90_000L, 0L));
    }
}
