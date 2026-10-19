package com.avrahamart.nightscreen;

import android.app.admin.DeviceAdminReceiver;

/**
 * Receives the explicitly user-enabled device-admin policy used only to lock
 * the screen when the selected screensaver activity duration expires.
 */
public class ScreenLockAdminReceiver extends DeviceAdminReceiver {
}
