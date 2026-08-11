package org.lts.tabnamedimmer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public final class LineOfSightCache {
    public static final LineOfSightCache INSTANCE = new LineOfSightCache();
    private static final int MAX_ENTRIES = 512;
    private final Map<Integer, Boolean> values = new HashMap<>();

    private LineOfSightCache() {
    }

    public boolean canSee(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player == player) {
            return minecraft.player == player;
        }
        if (values.size() >= MAX_ENTRIES) {
            values.clear();
        }
        return values.computeIfAbsent(player.getId(), ignored -> minecraft.player.hasLineOfSight(player));
    }

    public void clear() {
        values.clear();
    }
}
