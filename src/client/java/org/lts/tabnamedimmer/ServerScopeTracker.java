package org.lts.tabnamedimmer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

public final class ServerScopeTracker {
    private static volatile String currentScope = TabNameDimmerConfig.GLOBAL_SCOPE;

    private ServerScopeTracker() {
    }

    public static void update(Minecraft minecraft) {
        currentScope = resolve(minecraft);
    }

    public static void clear() {
        currentScope = TabNameDimmerConfig.GLOBAL_SCOPE;
    }

    public static String currentScope() {
        return currentScope;
    }

    public static String resolve(Minecraft minecraft) {
        if (minecraft == null) {
            return TabNameDimmerConfig.GLOBAL_SCOPE;
        }
        ServerData serverData = minecraft.getCurrentServer();
        if (serverData != null) {
            if (serverData.ip != null && !serverData.ip.isBlank()) {
                return "server:" + serverData.ip.trim().toLowerCase(java.util.Locale.ROOT);
            }
            if (serverData.name != null && !serverData.name.isBlank()) {
                return "server-name:" + serverData.name.trim();
            }
        }
        IntegratedServer integratedServer = minecraft.getSingleplayerServer();
        if (integratedServer != null && integratedServer.getWorldData() != null) {
            String levelName = integratedServer.getWorldData().getLevelName();
            if (levelName != null && !levelName.isBlank()) {
                return "singleplayer:" + levelName.trim();
            }
        }
        return TabNameDimmerConfig.GLOBAL_SCOPE;
    }
}
