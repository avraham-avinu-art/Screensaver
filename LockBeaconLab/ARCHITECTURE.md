# Clock Beacon Lab lifecycle architecture

The launcher Activity is only a setup and initial-start surface. It requests the overlay permission and starts BeaconKeeperService; it does not own, bind to, monitor, or periodically restart that service.

BeaconKeeperService runs in the dedicated Android application process `:keeper`. It owns the foreground notification, screen-state receiver, and lock-screen clock overlay in one lifecycle. The overlay is not a second started service and does not poll for the presence of a notification. Losing or hiding the notification is not used as a signal to destroy the overlay component.

The service promotes itself to foreground immediately in every onStartCommand invocation, including a null-intent restart, and returns START_STICKY. The manifest explicitly sets stopWithTask=false. Removing the launcher task therefore does not request service shutdown.

The app requires only FOREGROUND_SERVICE and SYSTEM_ALERT_WINDOW. It does not declare or use an AccessibilityService.

Android can still stop a package after an explicit Force Stop, and device manufacturers may impose additional background restrictions. A successful build cannot prove behavior on a physical device; task-removal behavior must be confirmed on the target phone.
