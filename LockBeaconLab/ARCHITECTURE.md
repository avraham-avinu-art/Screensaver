# Clock Beacon Lab lifecycle reference

This isolated test app uses two established open-source Android application patterns:

- Key Mapper (https://github.com/keymapperorg/KeyMapper): a system-bound AccessibilityService declared with BIND_ACCESSIBILITY_SERVICE. Here it is used only as the OS-managed lifecycle anchor; its configuration explicitly disables window-content retrieval, and the implementation ignores accessibility events.
- GPSLogger (https://github.com/mendhak/gpslogger): a started foreground service with stopWithTask=false, foreground promotion in both service creation and start, START_STICKY, and an explicit restart receiver after unexpected service destruction.

The implementation in this experiment is newly written for this app; it does not copy their source files. The screen clock is a TYPE_APPLICATION_OVERLAY owned by the foreground service. It no longer launches an Activity from the background.

This is a test build, not a claim that a regular Android application can override an explicit Force Stop. The Accessibility service must be enabled manually in Android Settings.
