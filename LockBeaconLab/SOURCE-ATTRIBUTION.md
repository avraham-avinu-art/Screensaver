# Source attribution

The foreground-service lifecycle implementation in `CounterService.kt` is copied from:

https://github.com/vinayjuyal07/ForegroundServiceExample

Its original foreground-service flow, notification construction, `START_STICKY`, and `onTaskRemoved` implementation are preserved. The Android package and the notification icon resource are adapted for this standalone test application.

The clock-only launcher screen and `ClockOverlayService` are additions for this test app. No AccessibilityService or Accessibility permission is present.
