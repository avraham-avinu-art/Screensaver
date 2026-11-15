package com.avrahamart.nightscreen;

/**
 * Restores the icon mapping that existed before the blanket icon replacement.
 * Only settings that previously fell back to the generic settings glyph use
 * their own newly drawn setting-specific vector.
 */
final class SettingIconRegistry {
    private static final String[] KEYS = {
            "showHebrew",
            "showGregorian",
            "preventLockWhileActive",
            "continueOnLockScreen",
            "autoStartOnDeviceLock",
            "autoStartOnBoot",
            "autoStartOnWake",
            "autoStartOnCharge",
            "autoStartOnIdle",
            "wakeOnTrackChange",
            "idleWaitSeconds",
            "autoStartOnlyWhenLocked",
            "noShutdownWhileCharging",
            "showDetails",
            "frameEnabled",
            "frameUseAlbumArt",
            "nightModeEnabled",
            "batteryProtectionEnabled",
            "useAndroidDreamForCharging",
            "pauseWhenExcludedAppActive",
            "preventAutoStartDuringUse",
            "settingsButtonAlwaysVisible",
            "hebrewDateFormat",
            "gregorianDateFormat",
            "fontFamily",
            "nightModeStartHour",
            "nightModeEndHour",
            "timeFormat",
            "clockDisplayMode",
            "clockDialStyle",
            "clockTextAlignment",
            "clockDatePosition",
            "screenSaverDuration",
            "chargingBatteryDisplayMode",
            "backgroundStyle",
            "playerDisplayMode",
            "playerControlsDisplayMode",
            "playerStartDurationSeconds",
            "playerPosition",
            "playerTextAlignment",
            "clockDateSize",
            "fontWeight",
            "screenBrightness",
            "playerSize",
            "frameWidth",
            "frameRadius",
            "batteryProtectionThreshold",
            "clockHorizontalOffset",
            "playerHorizontalOffset",
            "screenBackgroundColor",
            "fontColor",
            "frameColor",
            "resetAllSettings"
    };

    private SettingIconRegistry() {
    }

    static String[] keys() {
        return KEYS.clone();
    }

    static String iconNameForKey(String key) {
        switch (key) {
            case "showHebrew": return "hebrew-date";
            case "showGregorian": return "gregorian-date";
            case "preventLockWhileActive": return "power-off";
            case "continueOnLockScreen": return "shield";
            case "autoStartOnDeviceLock": return "lock-play";
            case "autoStartOnBoot": return "power";
            case "autoStartOnWake": return "screen-wake";
            case "autoStartOnCharge": return "usb";
            case "autoStartOnIdle": return "setting-auto-start-idle";
            case "wakeOnTrackChange": return "setting-track-change-wake";
            case "idleWaitSeconds": return "setting-idle-wait-duration";
            case "autoStartOnlyWhenLocked": return "lock-play";
            case "noShutdownWhileCharging": return "phone-charging";
            case "showDetails": return "artist";
            case "frameEnabled": return "frame-dashed";
            case "frameUseAlbumArt": return "setting-frame-use-album-art";
            case "nightModeEnabled": return "setting-night-mode-enabled";
            case "batteryProtectionEnabled": return "setting-battery-protection-enabled";
            case "useAndroidDreamForCharging": return "setting-use-android-dream-for-charging";
            case "pauseWhenExcludedAppActive": return "activity-history";
            case "preventAutoStartDuringUse": return "activity-history";
            case "settingsButtonAlwaysVisible": return "setting-settings-button-always-visible";
            case "hebrewDateFormat": return "setting-hebrew-date-format";
            case "gregorianDateFormat": return "setting-gregorian-date-format";
            case "fontFamily": return "font-family";
            case "nightModeStartHour": return "setting-night-mode-start-hour";
            case "nightModeEndHour": return "setting-night-mode-end-hour";
            case "timeFormat": return "setting-time-format";
            case "clockDisplayMode": return "clock";
            case "clockDialStyle": return "clock-dial";
            case "clockTextAlignment": return "setting-clock-text-alignment";
            case "clockDatePosition": return "position-arrows";
            case "screenSaverDuration": return "activity-history";
            case "chargingBatteryDisplayMode": return "battery-medium";
            case "backgroundStyle": return "setting-background-style";
            case "playerDisplayMode": return "media-toggle";
            case "playerControlsDisplayMode": return "progress-time-digital";
            case "playerStartDurationSeconds": return "setting-player-start-duration";
            case "playerPosition": return "panel-bottom";
            case "playerTextAlignment": return "setting-player-text-alignment";
            case "clockDateSize": return "clock-size";
            case "fontWeight": return "bold";
            case "screenBrightness": return "sun-dim";
            case "playerSize": return "player-size-resize";
            case "frameWidth": return "border-width";
            case "frameRadius": return "corners";
            case "batteryProtectionThreshold": return "setting-battery-protection-threshold";
            case "clockHorizontalOffset": return "setting-clock-horizontal-offset";
            case "playerHorizontalOffset": return "setting-player-horizontal-offset";
            case "screenBackgroundColor": return "palette";
            case "fontColor": return "paint-roller";
            case "frameColor": return "pipette";
            case "resetAllSettings": return "setting-reset-all";
            default:
                throw new IllegalArgumentException("Unknown setting key: " + key);
        }
    }
}
