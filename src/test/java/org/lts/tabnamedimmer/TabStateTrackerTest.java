package org.lts.tabnamedimmer;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
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

    private static PlayerInfo player(String name) {
        return player(name, 0);
    }

    private static PlayerInfo player(String name, int latency) {
        return new TestPlayerInfo(new GameProfile(
                UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name), latency);
    }

    private static final class TestPlayerInfo extends PlayerInfo {
        private final int latency;

        private TestPlayerInfo(GameProfile profile, int latency) {
            super(profile, false);
            this.latency = latency;
        }

        @Override
        public int getLatency() {
            return latency;
        }
    }
}
