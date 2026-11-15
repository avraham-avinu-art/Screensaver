package com.avrahamart.nightscreen;

/**
 * Central registry for the one-to-one mapping between persisted settings and
 * their individually designed vector drawable resources.
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
            "frameColor"
    };

    private SettingIconRegistry() {
    }

    static String[] keys() {
        return KEYS.clone();
    }

    static int drawableForKey(String key) {
        switch (key) {
            case "showHebrew": return R.drawable.icon_setting_show_hebrew;
            case "showGregorian": return R.drawable.icon_setting_show_gregorian;
            case "preventLockWhileActive": return R.drawable.icon_setting_prevent_lock_while_active;
            case "continueOnLockScreen": return R.drawable.icon_setting_continue_on_lock_screen;
            case "autoStartOnDeviceLock": return R.drawable.icon_setting_auto_start_on_device_lock;
            case "autoStartOnBoot": return R.drawable.icon_setting_auto_start_on_boot;
            case "autoStartOnWake": return R.drawable.icon_setting_auto_start_on_wake;
            case "autoStartOnCharge": return R.drawable.icon_setting_auto_start_on_charge;
            case "autoStartOnlyWhenLocked": return R.drawable.icon_setting_auto_start_only_when_locked;
            case "noShutdownWhileCharging": return R.drawable.icon_setting_no_shutdown_while_charging;
            case "showDetails": return R.drawable.icon_setting_show_details;
            case "frameEnabled": return R.drawable.icon_setting_frame_enabled;
            case "frameUseAlbumArt": return R.drawable.icon_setting_frame_use_album_art;
            case "nightModeEnabled": return R.drawable.icon_setting_night_mode_enabled;
            case "batteryProtectionEnabled": return R.drawable.icon_setting_battery_protection_enabled;
            case "useAndroidDreamForCharging": return R.drawable.icon_setting_use_android_dream_for_charging;
            case "pauseWhenExcludedAppActive": return R.drawable.icon_setting_pause_when_excluded_app_active;
            case "preventAutoStartDuringUse": return R.drawable.icon_setting_prevent_auto_start_during_use;
            case "settingsButtonAlwaysVisible": return R.drawable.icon_setting_settings_button_always_visible;
            case "hebrewDateFormat": return R.drawable.icon_setting_hebrew_date_format;
            case "gregorianDateFormat": return R.drawable.icon_setting_gregorian_date_format;
            case "fontFamily": return R.drawable.icon_setting_font_family;
            case "nightModeStartHour": return R.drawable.icon_setting_night_mode_start_hour;
            case "nightModeEndHour": return R.drawable.icon_setting_night_mode_end_hour;
            case "timeFormat": return R.drawable.icon_setting_time_format;
            case "clockDisplayMode": return R.drawable.icon_setting_clock_display_mode;
            case "clockDialStyle": return R.drawable.icon_setting_clock_dial_style;
            case "clockTextAlignment": return R.drawable.icon_setting_clock_text_alignment;
            case "clockDatePosition": return R.drawable.icon_setting_clock_date_position;
            case "screenSaverDuration": return R.drawable.icon_setting_screen_saver_duration;
            case "chargingBatteryDisplayMode": return R.drawable.icon_setting_charging_battery_display_mode;
            case "backgroundStyle": return R.drawable.icon_setting_background_style;
            case "playerDisplayMode": return R.drawable.icon_setting_player_display_mode;
            case "playerControlsDisplayMode": return R.drawable.icon_setting_player_controls_display_mode;
            case "playerPosition": return R.drawable.icon_setting_player_position;
            case "playerTextAlignment": return R.drawable.icon_setting_player_text_alignment;
            case "clockDateSize": return R.drawable.icon_setting_clock_date_size;
            case "fontWeight": return R.drawable.icon_setting_font_weight;
            case "screenBrightness": return R.drawable.icon_setting_screen_brightness;
            case "playerSize": return R.drawable.icon_setting_player_size;
            case "frameWidth": return R.drawable.icon_setting_frame_width;
            case "frameRadius": return R.drawable.icon_setting_frame_radius;
            case "batteryProtectionThreshold": return R.drawable.icon_setting_battery_protection_threshold;
            case "clockHorizontalOffset": return R.drawable.icon_setting_clock_horizontal_offset;
            case "playerHorizontalOffset": return R.drawable.icon_setting_player_horizontal_offset;
            case "screenBackgroundColor": return R.drawable.icon_setting_screen_background_color;
            case "fontColor": return R.drawable.icon_setting_font_color;
            case "frameColor": return R.drawable.icon_setting_frame_color;
            default:
                throw new IllegalArgumentException(
                        "No dedicated icon resource registered for setting: " + key);
        }
    }
}
