package org.lts.tabnamedimmer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TabStateTracker {
    public static final TabStateTracker INSTANCE = new TabStateTracker();

    private final Map<UUID, Float> displayWeights = new HashMap<>();
    private long lastUpdateTime = System.currentTimeMillis();

    public Stream<PlayerInfo> processPlayers(Stream<PlayerInfo> stream, Comparator<? super PlayerInfo> originalComparator) {
        TabNameDimmerConfig config = TabNameDimmerConfig.loadIfChanged();
        List<PlayerInfo> originalList = stream.sorted(originalComparator).collect(Collectors.toList());

        long now = System.currentTimeMillis();
        float dt = (now - lastUpdateTime) / 1000f;
        lastUpdateTime = now;
        
        // Cap dt to prevent massive jumps on lag spikes
        if (dt > 0.1f) dt = 0.1f;

        boolean shiftDown = TabNameDimmerClient.isShiftDown();

        if (config.displayMode == TabNameDimmerConfig.DisplayMode.FILTER && shiftDown) {
            return originalList.stream().filter(p -> config.shouldDim(p.getProfile().name()) == false);
        }

        if (config.displayMode == TabNameDimmerConfig.DisplayMode.ANIMATED_SORT) {
            for (int i = 0; i < originalList.size(); i++) {
                PlayerInfo info = originalList.get(i);
                UUID uuid = info.getProfile().id();
                
                float targetWeight = i;
                if (shiftDown && !config.shouldDim(info.getProfile().name())) {
                    targetWeight = i - originalList.size() * 10; // Highly negative weight to push to top
                }

                float currentWeight = displayWeights.getOrDefault(uuid, (float) i);
                
                // Lerp currentWeight towards targetWeight
                float diff = targetWeight - currentWeight;
                float move = diff * (config.animationSpeed * 10f) * dt * 60f; // Scale speed for 60fps baseline
                
                if (Math.abs(diff) < 0.1f) {
                    currentWeight = targetWeight;
                } else {
                    currentWeight += move;
                }
                
                displayWeights.put(uuid, currentWeight);
            }

            return originalList.stream().sorted(Comparator.comparingDouble(p -> displayWeights.getOrDefault(p.getProfile().id(), 0f)));
        }

        return originalList.stream();
    }
}
