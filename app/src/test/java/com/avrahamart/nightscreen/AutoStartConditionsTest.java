package com.avrahamart.nightscreen;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AutoStartConditionsTest {
    private static final String APP = "com.screensaver";
    private static final String LAUNCHER = "com.android.launcher";

    @Test
    public void globalProtectionBlocksAnyOtherForegroundApp() {
        assertTrue(blocks(
                setOf("com.example.mail"),
                true, false, Collections.emptySet()));
    }

    @Test
    public void selectedProtectionBlocksOnlyAnAppFromTheList() {
        assertTrue(blocks(
                setOf("com.example.video"),
                false, true, setOf("com.example.video")));
    }

    @Test
    public void selectedProtectionAllowsAppsNotInTheList() {
        assertFalse(blocks(
                setOf("com.example.mail"),
                false, true, setOf("com.example.video")));
    }

    @Test
    public void selectedProtectionDoesNothingWhenListIsEmpty() {
        assertFalse(blocks(
                setOf("com.example.mail"),
                false, true, Collections.emptySet()));
    }

    @Test
    public void globalProtectionAllowsLauncherAndSystemUi() {
        assertFalse(blocks(
                setOf(LAUNCHER, "com.android.systemui", APP, "android"),
                true, false, Collections.emptySet()));
    }

    @Test
    public void bothProtectionsApplyAsAUnion() {
        assertTrue(blocks(
                setOf("com.example.video"),
                true, true, setOf("com.example.mail")));
        assertTrue(blocks(
                setOf("com.example.mail"),
                true, true, setOf("com.example.video")));
    }

    @Test
    public void unknownForegroundStateFailsClosedWhenProtectionIsEnabled() {
        assertTrue(blocks(null, true, false, Collections.emptySet()));
        assertTrue(blocks(null, false, true, setOf("com.example.video")));
    }

    @Test
    public void noForegroundProtectionNeverBlocks() {
        assertFalse(blocks(
                setOf("com.example.mail"),
                false, false, Collections.emptySet()));
        assertFalse(blocks(
                null, false, false, Collections.emptySet()));
    }

    @Test
    public void globalPauseBlocksAutomaticStartWithoutEvaluatingOtherConditions() {
        final boolean[] evaluated = {false};
        assertFalse(AutoStartConditions.shouldAllowAutomaticActivation(true, () -> {
            evaluated[0] = true;
            return true;
        }));
        assertFalse(evaluated[0]);
    }

    @Test
    public void resumingRestoresTheExistingAutomaticStartRules() {
        assertTrue(AutoStartConditions.shouldAllowAutomaticActivation(false, () -> true));
        assertFalse(AutoStartConditions.shouldAllowAutomaticActivation(false, () -> false));
    }

    private static boolean blocks(Set<String> foreground,
                                  boolean global,
                                  boolean selected,
                                  Set<String> selectedPackages) {
        return AutoStartConditions.shouldBlockForForegroundApps(
                foreground, APP, LAUNCHER, global, selected, selectedPackages);
    }

    private static Set<String> setOf(String... values) {
        Set<String> result = new HashSet<>();
        Collections.addAll(result, values);
        return result;
    }
}
