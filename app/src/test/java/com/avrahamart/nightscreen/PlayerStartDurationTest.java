package com.avrahamart.nightscreen;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class PlayerStartDurationTest {
    @Test
    public void acceptsAllFourSupportedDurations() {
        assertEquals(3, PlayerStartDuration.normalize(3));
        assertEquals(6, PlayerStartDuration.normalize(6));
        assertEquals(10, PlayerStartDuration.normalize(10));
        assertEquals(15, PlayerStartDuration.normalize(15));
    }

    @Test
    public void defaultsUnsupportedValuesToSixSeconds() {
        assertEquals(6, PlayerStartDuration.normalize(0));
        assertEquals(6, PlayerStartDuration.normalize(5));
        assertEquals(6, PlayerStartDuration.normalize(30));
        assertEquals(6, PlayerStartDuration.normalize(-1));
    }

    @Test
    public void convertsTheSharedDurationToMilliseconds() {
        assertEquals(3_000L, PlayerStartDuration.millis(3));
        assertEquals(6_000L, PlayerStartDuration.millis(6));
        assertEquals(10_000L, PlayerStartDuration.millis(10));
        assertEquals(15_000L, PlayerStartDuration.millis(15));
    }
}
