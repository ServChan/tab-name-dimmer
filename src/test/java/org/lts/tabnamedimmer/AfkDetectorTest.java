package org.lts.tabnamedimmer;

import net.minecraft.network.chat.Component;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.scores.PlayerTeam;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfkDetectorTest {
    @Test
    void detectsSleepSuffixUsedByServerTabLists() {
        assertTrue(AfkDetector.isAfk("Sleeper", Component.literal("Sleeper zᶻ")));
        assertTrue(AfkDetector.isAfk("Sleeper", Component.literal("Sleeper ᴢᴢ")));
        assertTrue(AfkDetector.isAfk("Sleeper", Component.literal("Sleeper 💤")));
        assertTrue(AfkDetector.isAfk("Sleeper", Component.literal("Sleeper \uA423")));
    }

    @Test
    void detectsVanillaSquadSleepIconWhenServerReplacesTheAccountName() {
        assertTrue(AfkDetector.isAfk("Aell_", Component.literal("Aell \uA423")));
    }

    @Test
    void detectsVanillaSquadSleepIconInPlayerListObjective() {
        PlayerInfo info = new PlayerInfo(new GameProfile(
                UUID.nameUUIDFromBytes("Aell".getBytes(StandardCharsets.UTF_8)), "Aell"), false) {
            @Override
            public Component getTabListDisplayName() {
                return Component.literal("Aell");
            }

            @Override
            public PlayerTeam getTeam() {
                return null;
            }
        };

        assertTrue(AfkDetector.isAfk(info, Component.literal("\uA423")));
        assertFalse(AfkDetector.isAfk(info, Component.literal("\uA41F")));
    }

    @Test
    void detectsExplicitPrefixAndSuffixMarkers() {
        assertTrue(AfkDetector.isAfk("Player", Component.literal("[AFK] Player")));
        assertTrue(AfkDetector.isAfk("Player", Component.literal("Player (away)")));
    }

    @Test
    void doesNotMistakeNamesRanksOrSingleBadgesForAfk() {
        assertFalse(AfkDetector.isAfk("AfkMaster", Component.literal("AfkMaster")));
        assertFalse(AfkDetector.isAfk("Zed", Component.literal("[Clan AFK] Zed")));
        assertFalse(AfkDetector.isAfk("Zed", Component.literal("Zed z")));
        assertFalse(AfkDetector.isAfk("Zed", Component.literal("[VIP] Zed ✦")));
        assertFalse(AfkDetector.isAfk("Zed", Component.literal("Zed \uA41F")));
    }

    @Test
    void requiresAUsableServerDisplayName() {
        assertFalse(AfkDetector.isAfk("Player", null));
        assertFalse(AfkDetector.isAfk("Player", Component.literal("Someone else")));
    }
}
