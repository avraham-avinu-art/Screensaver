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
    public void restoresExistingIconsAndOnlyCreatesIconsForFormerFallbacks() {
        String[] keys = SettingIconRegistry.keys();
        Set<String> newSettingIcons = new HashSet<>();

        for (String key : keys) {
            String iconName = SettingIconRegistry.iconNameForKey(key);
            assertNotNull("Missing icon mapping for " + key, iconName);
            assertFalse("Empty icon mapping for " + key, iconName.trim().isEmpty());

            if (iconName.startsWith("setting-")) {
                assertTrue("New icon reused by another setting: " + key,
                        newSettingIcons.add(iconName));
            }
        }

        assertEquals(48, keys.length);
        assertEquals(16, newSettingIcons.size());
        assertEquals("hebrew-date", SettingIconRegistry.iconNameForKey("showHebrew"));
        assertEquals("paint-roller", SettingIconRegistry.iconNameForKey("fontColor"));
        assertEquals("activity-history",
                SettingIconRegistry.iconNameForKey("screenSaverDuration"));
        assertEquals("setting-night-mode-enabled",
                SettingIconRegistry.iconNameForKey("nightModeEnabled"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownSettingDoesNotFallBackToAGenericIcon() {
        SettingIconRegistry.iconNameForKey("unknownSetting");
    }
}
