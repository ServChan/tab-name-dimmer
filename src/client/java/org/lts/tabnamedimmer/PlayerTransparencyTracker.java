package org.lts.tabnamedimmer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps entity IDs to player names so that the render-time mixin can decide
 * whether a given avatar should be drawn translucent.
 *
 * <p>{@link org.lts.tabnamedimmer.mixin.AvatarTransparencyMixin} writes
 * entries during {@code extractRenderState}, and
 * {@link org.lts.tabnamedimmer.mixin.LivingEntityTransparencyMixin} reads
 * them during {@code getModelTint}.
 */
public final class PlayerTransparencyTracker {
    private static final int MAX_TRACKED_ENTITIES = 512;
    public static final PlayerTransparencyTracker INSTANCE = new PlayerTransparencyTracker();

    private final Map<Integer, String> entityIdToName = new ConcurrentHashMap<>();

    private PlayerTransparencyTracker() {}

    /** Called from AvatarRenderer.extractRenderState – records the player name for this entity. */
    public void put(int entityId, String playerName) {
        if (!entityIdToName.containsKey(entityId) && entityIdToName.size() >= MAX_TRACKED_ENTITIES) {
            entityIdToName.clear();
        }
        entityIdToName.put(entityId, playerName);
    }

    /** Called from LivingEntityRenderer.getModelTint – returns the player name, or null. */
    public String getName(int entityId) {
        return entityIdToName.get(entityId);
    }

    /** Periodic cleanup – removes entries not seen recently. */
    public void clear() {
        entityIdToName.clear();
    }
}
