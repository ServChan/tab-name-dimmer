package org.lts.tabnamedimmer;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TabStateTrackerTest {
    @Test
    void sortsGroupsByPriorityAndNamesInsideGroup() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.playerSortMode = TabNameDimmerConfig.PlayerSortMode.NAME;
        TabNameDimmerConfig.PlayerGroup friends = config.globalProfile.groups.getFirst();
        friends.name = "Friends";
        friends.priority = 10;
        friends.members = new java.util.ArrayList<>(List.of("Zed", "Amy"));
        TabNameDimmerConfig.PlayerGroup staff = TabNameDimmerConfig.PlayerGroup.defaultGroup();
        staff.name = "Staff";
        staff.priority = 100;
        staff.members.add("Mod");
        config.globalProfile.groups.add(staff);

        List<PlayerInfo> players = List.of(player("Other"), player("Zed"), player("Mod"), player("Amy"));
        List<String> sorted = TabStateTracker.sortedTrackedPlayers(players, config, TabNameDimmerConfig.GLOBAL_SCOPE)
                .stream().map(info -> info.getProfile().name()).toList();

        assertEquals(List.of("Mod", "Amy", "Zed"), sorted);
    }

    @Test
    void sortsPlayersByPingInsideGroup() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.playerSortMode = TabNameDimmerConfig.PlayerSortMode.PING;
        config.globalProfile.groups.getFirst().members.addAll(List.of("Slow", "Fast"));

        List<PlayerInfo> players = List.of(player("Slow", 180), player("Fast", 25));
        List<String> sorted = TabStateTracker.sortedTrackedPlayers(players, config, TabNameDimmerConfig.GLOBAL_SCOPE)
                .stream().map(info -> info.getProfile().name()).toList();

        assertEquals(List.of("Fast", "Slow"), sorted);
    }

    @Test
    void movesAfkPlayersBehindActivePlayers() {
        TabNameDimmerConfig config = trackedConfig();
        config.afkHandlingMode = TabNameDimmerConfig.AfkHandlingMode.MOVE_TO_END;
        PlayerTeam afkTeam = new Scoreboard().addPlayerTeam("afk");
        afkTeam.setPlayerSuffix(Component.literal(" \uA423"));

        List<PlayerInfo> players = List.of(
                player("Sleeper", 20, null, afkTeam),
                player("Active", 30, Component.literal("Active")));

        List<String> sorted = TabStateTracker.sortedTrackedPlayers(
                        players, config, TabNameDimmerConfig.GLOBAL_SCOPE)
                .stream().map(info -> info.getProfile().name()).toList();

        assertEquals(List.of("Active", "Sleeper"), sorted);
    }

    @Test
    void hidesAfkPlayersWhenConfigured() {
        TabNameDimmerConfig config = trackedConfig();
        config.afkHandlingMode = TabNameDimmerConfig.AfkHandlingMode.HIDE;

        List<PlayerInfo> players = List.of(
                player("Sleeper", 20, Component.literal("Sleeper [AFK]")),
                player("Active", 30, Component.literal("Active")));

        List<String> sorted = TabStateTracker.sortedTrackedPlayers(
                        players, config, TabNameDimmerConfig.GLOBAL_SCOPE)
                .stream().map(info -> info.getProfile().name()).toList();

        assertEquals(List.of("Active"), sorted);
    }

    @Test
    void hidesVanillaSquadAfkPlayerWhenDisplayNameDiffersFromAccountName() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.afkHandlingMode = TabNameDimmerConfig.AfkHandlingMode.HIDE;
        config.globalProfile.groups.getFirst().members.addAll(List.of("Aell_", "Active"));

        List<PlayerInfo> players = List.of(
                player("Aell_", 20, Component.literal("Aell \uA423")),
                player("Active", 30, Component.literal("Active")));

        List<String> sorted = TabStateTracker.sortedTrackedPlayers(
                        players, config, TabNameDimmerConfig.GLOBAL_SCOPE)
                .stream().map(info -> info.getProfile().name()).toList();

        assertEquals(List.of("Active"), sorted);
    }

    @Test
    void groupsWithEqualPriorityFollowListOrder() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.playerSortMode = TabNameDimmerConfig.PlayerSortMode.NAME;
        TabNameDimmerConfig.PlayerGroup zebra = config.globalProfile.groups.getFirst();
        zebra.name = "Zebra";
        zebra.members.add("Bob");
        TabNameDimmerConfig.PlayerGroup alpha = TabNameDimmerConfig.PlayerGroup.defaultGroup();
        alpha.name = "Alpha";
        alpha.members.add("Amy");
        config.globalProfile.groups.add(alpha);

        List<String> sorted = TabStateTracker.sortedTrackedPlayers(
                        List.of(player("Amy"), player("Bob")), config, TabNameDimmerConfig.GLOBAL_SCOPE)
                .stream().map(info -> info.getProfile().name()).toList();

        assertEquals(List.of("Bob", "Amy"), sorted);
    }

    @Test
    void masksAddMatchingPlayersToTheGroup() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.globalProfile.groups.getFirst().members.add("Clan_*");

        List<String> sorted = TabStateTracker.sortedTrackedPlayers(
                        List.of(player("Clan_Bob"), player("Other"), player("clan_amy")),
                        config, TabNameDimmerConfig.GLOBAL_SCOPE)
                .stream().map(info -> info.getProfile().name()).toList();

        assertEquals(List.of("Clan_Bob", "clan_amy"), sorted);
    }

    private static TabNameDimmerConfig trackedConfig() {
        TabNameDimmerConfig config = new TabNameDimmerConfig();
        config.globalProfile.groups.getFirst().members.addAll(List.of("Sleeper", "Active"));
        return config;
    }

    private static PlayerInfo player(String name) {
        return player(name, 0);
    }

    private static PlayerInfo player(String name, int latency) {
        return player(name, latency, null);
    }

    private static PlayerInfo player(String name, int latency, Component displayName) {
        return player(name, latency, displayName, null);
    }

    private static PlayerInfo player(String name, int latency, Component displayName, PlayerTeam team) {
        return new TestPlayerInfo(new GameProfile(
                UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name), latency, displayName, team);
    }

    private static final class TestPlayerInfo extends PlayerInfo {
        private final int latency;
        private final Component displayName;
        private final PlayerTeam team;

        private TestPlayerInfo(GameProfile profile, int latency, Component displayName, PlayerTeam team) {
            super(profile, false);
            this.latency = latency;
            this.displayName = displayName;
            this.team = team;
        }

        @Override
        public int getLatency() {
            return latency;
        }

        @Override
        public Component getTabListDisplayName() {
            return displayName;
        }

        @Override
        public PlayerTeam getTeam() {
            return team;
        }
    }
}
