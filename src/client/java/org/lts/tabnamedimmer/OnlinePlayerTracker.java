package org.lts.tabnamedimmer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class OnlinePlayerTracker {
    public static final OnlinePlayerTracker INSTANCE = new OnlinePlayerTracker();
    private final Map<UUID, String> previousPlayers = new LinkedHashMap<>();
    private String previousScope = "";
    private int ticks;
    private boolean initialized;

    private OnlinePlayerTracker() {
    }

    public void tick(Minecraft minecraft) {
        if (++ticks < 20) {
            return;
        }
        ticks = 0;
        if (minecraft.getConnection() == null || minecraft.player == null) {
            clear();
            return;
        }
        String scope = ServerScopeTracker.currentScope();
        Map<UUID, String> currentPlayers = new LinkedHashMap<>();
        for (PlayerInfo info : minecraft.getConnection().getListedOnlinePlayers()) {
            currentPlayers.put(info.getProfile().id(), info.getProfile().name());
        }
        if (!scope.equals(previousScope) || !initialized) {
            previousScope = scope;
            previousPlayers.clear();
            previousPlayers.putAll(currentPlayers);
            initialized = true;
            return;
        }

        TabNameDimmerConfig config = TabNameDimmerConfig.current();
        if (config.enabled && config.notificationsEnabled) {
            currentPlayers.forEach((uuid, name) -> {
                if (!previousPlayers.containsKey(uuid) && config.findMatch(name, scope) != null) {
                    minecraft.player.sendSystemMessage(Component.translatable("tabnamedimmer.notification.joined", name));
                }
            });
            previousPlayers.forEach((uuid, name) -> {
                if (!currentPlayers.containsKey(uuid) && config.findMatch(name, scope) != null) {
                    minecraft.player.sendSystemMessage(Component.translatable("tabnamedimmer.notification.left", name));
                }
            });
        }
        previousPlayers.clear();
        previousPlayers.putAll(currentPlayers);
    }

    public void clear() {
        previousPlayers.clear();
        previousScope = "";
        ticks = 0;
        initialized = false;
    }
}
