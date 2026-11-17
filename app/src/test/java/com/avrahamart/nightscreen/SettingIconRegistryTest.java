package com.avrahamart.nightscreen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public final class SettingIconRegistryTest {
    @Test
    public void everySettingHasItsOwnDedicatedIcon() {
        String[] keys = SettingIconRegistry.keys();
        Set<String> uniqueIconNames = new HashSet<>();

        for (String key : keys) {
            String iconName = SettingIconRegistry.iconNameForKey(key);
            assertNotNull("Missing icon mapping for " + key, iconName);
            assertFalse("Empty icon mapping for " + key, iconName.trim().isEmpty());
            assertTrue("Icon is shared by more than one setting: " + iconName,
                    uniqueIconNames.add(iconName));
        }

        assertEquals(53, keys.length);
        assertEquals("Every setting must map to its own icon", 53, uniqueIconNames.size());

        assertEquals("hebrew-date", SettingIconRegistry.iconNameForKey("showHebrew"));
        assertEquals("paint-roller", SettingIconRegistry.iconNameForKey("fontColor"));
        assertEquals("setting-screensaver-duration",
                SettingIconRegistry.iconNameForKey("screenSaverDuration"));
        assertEquals("setting-night-mode-enabled",
                SettingIconRegistry.iconNameForKey("nightModeEnabled"));
        assertEquals("setting-auto-start-idle",
                SettingIconRegistry.iconNameForKey("autoStartOnIdle"));
        assertEquals("setting-track-change-wake",
                SettingIconRegistry.iconNameForKey("wakeOnTrackChange"));
        assertEquals("setting-idle-wait-duration",
                SettingIconRegistry.iconNameForKey("idleWaitSeconds"));
        assertEquals("setting-player-start-duration",
                SettingIconRegistry.iconNameForKey("playerStartDurationSeconds"));
        assertEquals("setting-only-when-locked",
                SettingIconRegistry.iconNameForKey("autoStartOnlyWhenLocked"));
        assertEquals("setting-pause-on-excluded-app",
                SettingIconRegistry.iconNameForKey("pauseWhenExcludedAppActive"));
        assertEquals("setting-prevent-autostart-during-use",
                SettingIconRegistry.iconNameForKey("preventAutoStartDuringUse"));
        assertEquals("setting-reset-all",
                SettingIconRegistry.iconNameForKey("resetAllSettings"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownSettingDoesNotFallBackToAGenericIcon() {
        SettingIconRegistry.iconNameForKey("unknownSetting");
    }
}
