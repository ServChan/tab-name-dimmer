package org.lts.tabnamedimmer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

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
    void restoresMissingPrimaryConfigurationFromBackup() throws Exception {
        TabNameDimmerConfig.useConfigDirectoryForTests(temporaryDirectory);
        try {
            TabNameDimmerConfig saved = new TabNameDimmerConfig();
            saved.globalProfile.groups.getFirst().members.add("Alice");
            assertTrue(TabNameDimmerConfig.save(saved));
            saved.globalProfile.groups.getFirst().members.add("Bob");
            assertTrue(TabNameDimmerConfig.save(saved));
            Files.delete(temporaryDirectory.resolve("tab-name-dimmer.json"));

            TabNameDimmerConfig loaded = TabNameDimmerConfig.load();

            assertEquals(List.of("Alice"), loaded.globalProfile.groups.getFirst().members);
            assertTrue(Files.isRegularFile(temporaryDirectory.resolve("tab-name-dimmer.json")));
            assertEquals(List.of("Alice"), TabNameDimmerConfig.load().globalProfile.groups.getFirst().members);
        } finally {
            TabNameDimmerConfig.useConfigDirectoryForTests(null);
        }
    }

    @Test
    void restoresCorruptPrimaryConfigurationFromBackup() throws Exception {
        TabNameDimmerConfig.useConfigDirectoryForTests(temporaryDirectory);
        try {
            TabNameDimmerConfig saved = new TabNameDimmerConfig();
            saved.globalProfile.groups.getFirst().members.add("Alice");
            assertTrue(TabNameDimmerConfig.save(saved));
            assertTrue(TabNameDimmerConfig.save(saved));
            Files.writeString(temporaryDirectory.resolve("tab-name-dimmer.json"), "{ broken");

            TabNameDimmerConfig loaded = TabNameDimmerConfig.load();

            assertEquals(List.of("Alice"), loaded.globalProfile.groups.getFirst().members);
            assertTrue(Files.isRegularFile(temporaryDirectory.resolve("tab-name-dimmer.json.corrupt")));
        } finally {
            TabNameDimmerConfig.useConfigDirectoryForTests(null);
        }
    }

    @Test
    void createsDefaultsWhenNoConfigurationExists() {
        TabNameDimmerConfig.useConfigDirectoryForTests(temporaryDirectory);
        try {
            TabNameDimmerConfig loaded = TabNameDimmerConfig.load();

            assertEquals(1, loaded.globalProfile.groups.size());
            assertTrue(Files.isRegularFile(temporaryDirectory.resolve("tab-name-dimmer.json")));
        } finally {
            TabNameDimmerConfig.useConfigDirectoryForTests(null);
        }
    }

    @Test
    void reloadsConfigurationChangedOnDisk() throws Exception {
        TabNameDimmerConfig.useConfigDirectoryForTests(temporaryDirectory);
        try {
            assertTrue(TabNameDimmerConfig.save(new TabNameDimmerConfig()));
            Path file = temporaryDirectory.resolve("tab-name-dimmer.json");
            Files.writeString(file, Files.readString(file).replace("\"members\": []", "\"members\": [\"Carol\"]"));
            Files.setLastModifiedTime(file, FileTime.fromMillis(Files.getLastModifiedTime(file).toMillis() + 5000L));

            TabNameDimmerConfig.reloadIfChanged();

            assertEquals(List.of("Carol"), TabNameDimmerConfig.current().globalProfile.groups.getFirst().members);
        } finally {
            TabNameDimmerConfig.useConfigDirectoryForTests(null);
        }
    }

    @Test
    void masksMatchNamesUsingWildcards() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.globalProfile.groups.getFirst().members.addAll(List.of("Clan_*", "Bot?", "a.b*"));

        assertNotNull(config.findMatch("Clan_Steve", TabNameDimmerConfig.GLOBAL_SCOPE));
        assertNotNull(config.findMatch("clan_", TabNameDimmerConfig.GLOBAL_SCOPE));
        assertNotNull(config.findMatch("Bot7", TabNameDimmerConfig.GLOBAL_SCOPE));
        assertNull(config.findMatch("Bot77", TabNameDimmerConfig.GLOBAL_SCOPE));
        assertNotNull(config.findMatch("a.bc", TabNameDimmerConfig.GLOBAL_SCOPE));
        assertNull(config.findMatch("axbc", TabNameDimmerConfig.GLOBAL_SCOPE));
        assertNull(config.findMatch("MyClan_Steve", TabNameDimmerConfig.GLOBAL_SCOPE));

        config.caseSensitive = true;
        assertNull(config.findMatch("clan_Steve", TabNameDimmerConfig.GLOBAL_SCOPE));
    }

    @Test
    void exactAndMaskMatchesRespectPriorityAndListOrder() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.globalProfile.groups = new ArrayList<>();
        TabNameDimmerConfig.PlayerGroup masked = TabNameDimmerConfig.PlayerGroup.defaultGroup();
        masked.name = "Masked";
        masked.priority = 5;
        masked.members.add("Clan_*");
        TabNameDimmerConfig.PlayerGroup exact = TabNameDimmerConfig.PlayerGroup.defaultGroup();
        exact.name = "Exact";
        exact.priority = 5;
        exact.members.add("Clan_Bob");
        config.globalProfile.groups.add(masked);
        config.globalProfile.groups.add(exact);

        assertEquals("Masked", config.findMatch("Clan_Bob", TabNameDimmerConfig.GLOBAL_SCOPE).group().name);

        exact.priority = 6;
        assertEquals("Exact", config.findMatch("Clan_Bob", TabNameDimmerConfig.GLOBAL_SCOPE).group().name);
        assertEquals("Masked", config.findMatch("Clan_Amy", TabNameDimmerConfig.GLOBAL_SCOPE).group().name);

        masked.enabled = false;
        assertNull(config.findMatch("Clan_Amy", TabNameDimmerConfig.GLOBAL_SCOPE));
    }

    @Test
    void matchIndexFollowsMemberEdits() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        TabNameDimmerConfig.PlayerGroup group = config.globalProfile.groups.getFirst();
        assertNull(config.findMatch("Dave", TabNameDimmerConfig.GLOBAL_SCOPE));

        group.members.add("Dave");
        assertNotNull(config.findMatch("Dave", TabNameDimmerConfig.GLOBAL_SCOPE));

        group.members = new ArrayList<>(List.of("Eve"));
        assertNull(config.findMatch("Dave", TabNameDimmerConfig.GLOBAL_SCOPE));
        assertNotNull(config.findMatch("eve", TabNameDimmerConfig.GLOBAL_SCOPE));

        config.enabled = false;
        assertNull(config.findMatch("Eve", TabNameDimmerConfig.GLOBAL_SCOPE));
    }

    @Test
    void comparesProfilesByGroupContent() {
        TabNameDimmerConfig.Profile profile = TabNameDimmerConfig.Profile.defaultProfile();
        profile.groups.getFirst().members.add("Alice");
        TabNameDimmerConfig.Profile copy = profile.copy();
        copy.name = "server:example.org";

        assertTrue(copy.sameGroupsAs(profile));
        copy.groups.getFirst().glowingEnabled = true;
        assertFalse(copy.sameGroupsAs(profile));
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
