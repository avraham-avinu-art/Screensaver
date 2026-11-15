package com.avrahamart.nightscreen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public final class SettingIconRegistryTest {
    @Test
    public void everySettingHasItsOwnDrawableResource() {
        String[] keys = SettingIconRegistry.keys();
        Set<Integer> drawableIds = new HashSet<>();

        for (String key : keys) {
            int drawableId = SettingIconRegistry.drawableForKey(key);
            assertNotEquals("Missing drawable for " + key, 0, drawableId);
            assertTrue("Drawable reused by multiple settings: " + key,
                    drawableIds.add(drawableId));
        }

        assertEquals(48, keys.length);
        assertEquals(keys.length, drawableIds.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownSettingDoesNotFallBackToAGenericIcon() {
        SettingIconRegistry.drawableForKey("unknownSetting");
    }
}
