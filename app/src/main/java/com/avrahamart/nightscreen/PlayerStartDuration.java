package com.avrahamart.nightscreen;

/** Shared duration choices for all player and track-change start-of-song displays. */
final class PlayerStartDuration {
    private PlayerStartDuration() { }

    static int normalize(int seconds) {
        switch (seconds) {
            case 3:
            case 6:
            case 10:
            case 15:
                return seconds;
            default:
                return 6;
        }
    }

    static long millis(int seconds) {
        return normalize(seconds) * 1000L;
    }
}
