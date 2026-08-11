package org.lts.tabnamedimmer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerNameStylerTest {
    @Test
    void groupWithNameColoringDisabledDoesNotUnboxNull() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        TabNameDimmerConfig.PlayerGroup group = new TabNameDimmerConfig.PlayerGroup();
        group.colorizeNames = false;

        assertNull(PlayerNameStyler.resolveColor(config, new TabNameDimmerConfig.Match(group, 0)));
    }

    @Test
    void resolvesConfiguredColorsForUntrackedAndColoredGroupNames() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.dimColor = 0x123456;
        TabNameDimmerConfig.PlayerGroup group = new TabNameDimmerConfig.PlayerGroup();
        group.color = 0xABCDEF;
        group.colorizeNames = true;

        assertEquals(0x123456, PlayerNameStyler.resolveColor(config, null));
        assertEquals(0xABCDEF,
                PlayerNameStyler.resolveColor(config, new TabNameDimmerConfig.Match(group, 0)));
    }
}
