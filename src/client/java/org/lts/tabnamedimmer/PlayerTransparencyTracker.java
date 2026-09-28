package org.lts.tabnamedimmer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerTransparencyTracker {
    private static final int MAX_TRACKED_ENTITIES = 512;
    public static final PlayerTransparencyTracker INSTANCE = new PlayerTransparencyTracker();

    private final Map<Integer, String> entityIdToName = new ConcurrentHashMap<>();

    private PlayerTransparencyTracker() {}

    public void put(int entityId, String playerName) {
        if (!entityIdToName.containsKey(entityId) && entityIdToName.size() >= MAX_TRACKED_ENTITIES) {
            entityIdToName.clear();
        }
        entityIdToName.put(entityId, playerName);
    }

    public String getName(int entityId) {
        return entityIdToName.get(entityId);
    }

    public void clear() {
        entityIdToName.clear();
    }
}
