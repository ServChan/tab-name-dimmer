package org.lts.tabnamedimmer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TabNameDimmerConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void migratesLegacyAllowlistWithoutLosingNames() {
        TabNameDimmerConfig config = TabNameDimmerConfig.fromJsonForTests("""
                {
                  "enabled": true,
                  "caseSensitive": false,
                  "glowingEnabled": true,
                  "allowedNames": ["Alice", "Bob"]
                }
                """);

        assertEquals(TabNameDimmerConfig.CURRENT_SCHEMA_VERSION, config.schemaVersion);
        assertEquals(java.util.List.of("Alice", "Bob"), config.globalProfile.groups.getFirst().members);
        assertTrue(config.globalProfile.groups.getFirst().glowingEnabled);
    }

    @Test
    void serverProfileOverridesGlobalAndFallsBackWhenMissing() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.globalProfile.groups.getFirst().members.add("GlobalPlayer");
        TabNameDimmerConfig.Profile server = new TabNameDimmerConfig.Profile("Example");
        server.groups.add(TabNameDimmerConfig.PlayerGroup.defaultGroup());
        server.groups.getFirst().members.add("ServerPlayer");
        config.serverProfiles.put("server:example.org", server);
        config = TabNameDimmerConfig.sanitizeForTests(config);

        assertNotNull(config.findMatch("GlobalPlayer", "server:other.org"));
        assertNull(config.findMatch("GlobalPlayer", "server:example.org"));
        assertNotNull(config.findMatch("ServerPlayer", "server:example.org"));
    }

    @Test
    void highestPriorityMatchingGroupWins() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.globalProfile.groups = new ArrayList<>();
        TabNameDimmerConfig.PlayerGroup low = TabNameDimmerConfig.PlayerGroup.defaultGroup();
        low.name = "Low";
        low.priority = 1;
        low.members.add("Alice");
        TabNameDimmerConfig.PlayerGroup high = TabNameDimmerConfig.PlayerGroup.defaultGroup();
        high.name = "High";
        high.priority = 10;
        high.members.add("alice");
        config.globalProfile.groups.add(low);
        config.globalProfile.groups.add(high);

        TabNameDimmerConfig.Match match = config.findMatch("ALICE", TabNameDimmerConfig.GLOBAL_SCOPE);
        assertNotNull(match);
        assertEquals("High", match.group().name);
    }

    @Test
    void sanitizeClampsVisualAndHudValues() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.dimOpacity = Float.NaN;
        config.animationSpeed = 99.0F;
        config.hudColumns = 99;
        config.hudMaxRows = 0;
        config = TabNameDimmerConfig.sanitizeForTests(config);

        assertEquals(0.3F, config.dimOpacity);
        assertEquals(1.0F, config.animationSpeed);
        assertEquals(4, config.hudColumns);
        assertEquals(1, config.hudMaxRows);
    }

    @Test
    void copyPreservesNotificationOptions() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.compactNotifications = true;
        config.notificationSoundsEnabled = true;
        config.afkHandlingMode = TabNameDimmerConfig.AfkHandlingMode.HIDE;

        TabNameDimmerConfig copy = config.copy();

        assertTrue(copy.compactNotifications);
        assertTrue(copy.notificationSoundsEnabled);
        assertEquals(TabNameDimmerConfig.AfkHandlingMode.HIDE, copy.afkHandlingMode);
    }

    @Test
    void rejectsFutureSchemaInsteadOfSilentlyDowngradingIt() {
        assertThrows(IllegalArgumentException.class,
                () -> TabNameDimmerConfig.fromJsonForTests("{\"schemaVersion\":999}"));
    }

    @Test
    void copiesMalformedProfilesWithoutCrashing() {
        TabNameDimmerConfig.Profile profile = new TabNameDimmerConfig.Profile("Broken");
        profile.groups = null;
        TabNameDimmerConfig.PlayerGroup group = new TabNameDimmerConfig.PlayerGroup();
        group.members = null;

        assertTrue(profile.copy().groups.isEmpty());
        assertTrue(group.copy().members.isEmpty());

        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.globalProfile = null;
        config.serverProfiles = null;
        assertNotNull(config.copy().globalProfile);
        assertTrue(config.copy().serverProfiles.isEmpty());
    }

    @Test
    void exportsAndImportsCompleteProfile() {
        TabNameDimmerConfig.Profile profile = TabNameDimmerConfig.Profile.defaultProfile();
        profile.name = "Friends";
        profile.groups.getFirst().color = 0x123456;
        profile.groups.getFirst().members.addAll(java.util.List.of("Alice", "Bob"));
        Path file = temporaryDirectory.resolve("profile.json");

        assertTrue(TabNameDimmerConfig.exportProfile(profile, file));
        TabNameDimmerConfig.Profile imported = TabNameDimmerConfig.importProfile(file);

        assertNotNull(imported);
        assertEquals("Friends", imported.name);
        assertEquals(0x123456, imported.groups.getFirst().color);
        assertEquals(java.util.List.of("Alice", "Bob"), imported.groups.getFirst().members);
    }
}
