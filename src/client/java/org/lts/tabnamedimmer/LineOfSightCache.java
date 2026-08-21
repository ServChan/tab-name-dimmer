package org.lts.tabnamedimmer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class LineOfSightCache {
    public static final LineOfSightCache INSTANCE = new LineOfSightCache();
    private static final int MAX_ENTRIES = 512;
    private final ConcurrentMap<Integer, Boolean> values = new ConcurrentHashMap<>();
    private volatile int localPlayerId = Integer.MIN_VALUE;

    private LineOfSightCache() {
    }

    /** Refreshes visibility on the client tick thread; render workers only read the snapshot. */
    public void refresh(Minecraft minecraft) {
        values.clear();
        localPlayerId = Integer.MIN_VALUE;
        if (minecraft == null || minecraft.player == null || minecraft.level == null
                || !TabNameDimmerClient.isActivationActive() || !TabNameDimmerClient.isTabListOpen()) {
            return;
        }

        Player localPlayer = minecraft.player;
        localPlayerId = localPlayer.getId();
        TabNameDimmerConfig config = TabNameDimmerConfig.current();
        String scope = ServerScopeTracker.currentScope();
        for (Player player : minecraft.level.players()) {
            if (player == localPlayer || values.size() >= MAX_ENTRIES) {
                continue;
            }
            TabNameDimmerConfig.Match match = config.findMatch(player.getGameProfile().name(), scope);
            if (match != null && match.group().glowingEnabled) {
                values.put(player.getId(), localPlayer.hasLineOfSight(player));
            }
        }
    }

    public boolean canSee(Player player) {
        if (player == null) {
            return false;
        }
        return localPlayerId == player.getId() || Boolean.TRUE.equals(values.get(player.getId()));
    }

    public void clear() {
        values.clear();
        localPlayerId = Integer.MIN_VALUE;
    }
}
