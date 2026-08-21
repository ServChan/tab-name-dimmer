package org.lts.tabnamedimmer;

import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TabStateTracker {
    public static final TabStateTracker INSTANCE = new TabStateTracker();

    private final Map<UUID, Float> displayWeights = new HashMap<>();
    private long lastUpdateTimeNanos = System.nanoTime();

    public Stream<PlayerInfo> processPlayers(Stream<PlayerInfo> stream,
                                             Comparator<? super PlayerInfo> originalComparator) {
        TabNameDimmerConfig config = TabNameDimmerConfig.current();
        List<PlayerInfo> original = stream.sorted(originalComparator).collect(Collectors.toList());
        if (!config.enabled) {
            clear();
            return original.stream();
        }

        String scope = ServerScopeTracker.currentScope();
        boolean active = TabNameDimmerClient.isActivationActive();
        Map<PlayerInfo, Boolean> afkPlayers = afkPlayers(original, config);
        List<PlayerInfo> candidates = config.afkHandlingMode == TabNameDimmerConfig.AfkHandlingMode.HIDE
                ? original.stream().filter(info -> !afkPlayers.getOrDefault(info, false)).toList()
                : original;
        Map<PlayerInfo, TabNameDimmerConfig.Match> matches = new IdentityHashMap<>();
        for (PlayerInfo info : candidates) {
            matches.put(info, config.findMatch(info.getProfile().name(), scope));
        }

        Comparator<PlayerInfo> groupedComparator = groupedComparator(config, original, matches);
        Map<PlayerInfo, Integer> originalIndexes = indexMap(original);
        Comparator<PlayerInfo> originalOrder = Comparator.comparingInt(originalIndexes::get);
        Comparator<PlayerInfo> activeComparator = withAfkLast(groupedComparator, afkPlayers, config);
        Comparator<PlayerInfo> inactiveComparator = withAfkLast(originalOrder, afkPlayers, config);
        if (config.displayMode == TabNameDimmerConfig.DisplayMode.FILTER && active) {
            return candidates.stream().filter(info -> matches.get(info) != null).sorted(activeComparator);
        }

        if (config.displayMode != TabNameDimmerConfig.DisplayMode.ANIMATED_SORT) {
            displayWeights.clear();
            if (config.afkHandlingMode == TabNameDimmerConfig.AfkHandlingMode.MOVE_TO_END) {
                return candidates.stream().sorted(inactiveComparator);
            }
            return candidates.stream();
        }

        List<PlayerInfo> desired = active
                ? candidates.stream().sorted(activeComparator).toList()
                : candidates.stream().sorted(inactiveComparator).toList();
        Map<UUID, Integer> targets = new HashMap<>();
        for (int i = 0; i < desired.size(); i++) {
            targets.put(desired.get(i).getProfile().id(), i);
        }

        long currentNanos = System.nanoTime();
        float dt = Math.max(0.0F, Math.min(0.1F,
                (currentNanos - lastUpdateTimeNanos) / 1_000_000_000F));
        lastUpdateTimeNanos = currentNanos;
        float fraction = Math.min(1.0F, config.animationSpeed * 600.0F * dt);

        displayWeights.keySet().retainAll(targets.keySet());
        for (int i = 0; i < candidates.size(); i++) {
            PlayerInfo info = candidates.get(i);
            UUID uuid = info.getProfile().id();
            float target = targets.getOrDefault(uuid, i);
            float current = displayWeights.getOrDefault(uuid, (float) i);
            float difference = target - current;
            current = Math.abs(difference) < 0.01F ? target : current + difference * fraction;
            displayWeights.put(uuid, current);
        }

        return candidates.stream().sorted(Comparator
                .comparingDouble((PlayerInfo info) -> displayWeights.getOrDefault(info.getProfile().id(), 0.0F))
                .thenComparingInt(originalIndexes::get));
    }

    static Comparator<PlayerInfo> groupedComparator(TabNameDimmerConfig config,
                                                     List<PlayerInfo> original,
                                                     Map<PlayerInfo, TabNameDimmerConfig.Match> matches) {
        Map<PlayerInfo, Integer> originalIndexes = indexMap(original);
        Comparator<PlayerInfo> insideGroup = switch (config.playerSortMode) {
            case ORIGINAL -> Comparator.comparingInt(originalIndexes::get);
            case NAME -> Comparator.comparing(info -> info.getProfile().name(), String.CASE_INSENSITIVE_ORDER);
            case PING -> Comparator.comparingInt(PlayerInfo::getLatency)
                    .thenComparing(info -> info.getProfile().name(), String.CASE_INSENSITIVE_ORDER);
        };
        return Comparator
                .comparingInt((PlayerInfo info) -> matches.get(info) == null ? Integer.MIN_VALUE : matches.get(info).priority())
                .reversed()
                .thenComparing(info -> matches.get(info) == null ? 1 : 0)
                .thenComparing(info -> matches.get(info) == null ? "" : matches.get(info).group().name,
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(insideGroup);
    }

    public static List<PlayerInfo> sortedTrackedPlayers(java.util.Collection<PlayerInfo> players,
                                                        TabNameDimmerConfig config,
                                                        String scope) {
        List<PlayerInfo> original = List.copyOf(players);
        Map<PlayerInfo, Boolean> afkPlayers = afkPlayers(original, config);
        Map<PlayerInfo, TabNameDimmerConfig.Match> matches = new IdentityHashMap<>();
        for (PlayerInfo info : original) {
            matches.put(info, config.findMatch(info.getProfile().name(), scope));
        }
        return original.stream()
                .filter(info -> matches.get(info) != null)
                .filter(info -> config.afkHandlingMode != TabNameDimmerConfig.AfkHandlingMode.HIDE
                        || !afkPlayers.getOrDefault(info, false))
                .sorted(withAfkLast(groupedComparator(config, original, matches), afkPlayers, config))
                .toList();
    }

    private static Map<PlayerInfo, Boolean> afkPlayers(List<PlayerInfo> players,
                                                        TabNameDimmerConfig config) {
        Map<PlayerInfo, Boolean> afkPlayers = new IdentityHashMap<>();
        if (config.afkHandlingMode != TabNameDimmerConfig.AfkHandlingMode.SHOW) {
            for (PlayerInfo info : players) {
                afkPlayers.put(info, AfkDetector.isAfk(info));
            }
        }
        return afkPlayers;
    }

    private static Comparator<PlayerInfo> withAfkLast(Comparator<PlayerInfo> comparator,
                                                       Map<PlayerInfo, Boolean> afkPlayers,
                                                       TabNameDimmerConfig config) {
        if (config.afkHandlingMode != TabNameDimmerConfig.AfkHandlingMode.MOVE_TO_END) {
            return comparator;
        }
        return Comparator.comparing((PlayerInfo info) -> afkPlayers.getOrDefault(info, false))
                .thenComparing(comparator);
    }

    private static Map<PlayerInfo, Integer> indexMap(List<PlayerInfo> players) {
        Map<PlayerInfo, Integer> indexes = new IdentityHashMap<>();
        for (int i = 0; i < players.size(); i++) {
            indexes.put(players.get(i), i);
        }
        return indexes;
    }

    public void clear() {
        displayWeights.clear();
        lastUpdateTimeNanos = System.nanoTime();
    }
}
