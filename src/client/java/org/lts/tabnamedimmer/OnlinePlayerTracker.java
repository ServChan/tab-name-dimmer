package org.lts.tabnamedimmer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class OnlinePlayerTracker {
    public static final OnlinePlayerTracker INSTANCE = new OnlinePlayerTracker();

    public static final Component JOIN_ICON = Component.literal("[+] ")
            .withStyle(style -> style.withColor(0x55FF55).withBold(true));

    public static final Component LEAVE_ICON = Component.literal("[-] ")
            .withStyle(style -> style.withColor(0xFF5555).withBold(true));

    public record TrackedPlayer(UUID uuid, String name, Component serverDisplayName) {}

    enum PresenceChange {
        JOINED, LEFT
    }

    private final Map<UUID, TrackedPlayer> previousPlayers = new LinkedHashMap<>();
    private String previousScope = "";
    private int ticks;
    private boolean initialized;

    OnlinePlayerTracker() {
    }

    public void tick(Minecraft minecraft) {
        if (minecraft == null) {
            clear();
            return;
        }
        if (++ticks < 20) {
            return;
        }
        ticks = 0;
        if (minecraft.getConnection() == null || minecraft.player == null) {
            clear();
            return;
        }
        String scope = ServerScopeTracker.currentScope();
        Map<UUID, TrackedPlayer> currentPlayers = new LinkedHashMap<>();
        for (PlayerInfo info : minecraft.getConnection().getListedOnlinePlayers()) {
            if (info == null || info.getProfile() == null || info.getProfile().id() == null
                    || info.getProfile().name() == null || info.getProfile().name().isBlank()) {
                continue;
            }
            UUID uuid = info.getProfile().id();
            String name = info.getProfile().name();
            Component serverDisplayName = info.getTabListDisplayName();
            currentPlayers.put(uuid, new TrackedPlayer(uuid, name, serverDisplayName));
        }
        processTick(scope, currentPlayers, TabNameDimmerConfig.current(),
                message -> minecraft.player.sendSystemMessage(message),
                change -> minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                        SoundEvents.UI_BUTTON_CLICK, change == PresenceChange.JOINED ? 1.2F : 0.8F)));
    }

    void processTick(String scope, Map<UUID, TrackedPlayer> currentPlayers, TabNameDimmerConfig config,
                     Consumer<Component> messageSender) {
        processTick(scope, currentPlayers, config, messageSender, ignored -> {});
    }

    void processTick(String scope, Map<UUID, TrackedPlayer> currentPlayers, TabNameDimmerConfig config,
                     Consumer<Component> messageSender, Consumer<PresenceChange> soundPlayer) {
        if (!scope.equals(previousScope) || !initialized) {
            previousScope = scope;
            previousPlayers.clear();
            previousPlayers.putAll(currentPlayers);
            initialized = true;
            return;
        }

        if (config.enabled && config.notificationsEnabled) {
            currentPlayers.forEach((uuid, player) -> {
                if (!previousPlayers.containsKey(uuid) && config.findMatch(player.name(), scope) != null) {
                    Component styledName = formatPlayerName(player.name(), player.serverDisplayName(), config, scope);
                    messageSender.accept(createNotification(PresenceChange.JOINED, styledName, config.compactNotifications));
                    playNotificationSound(config, soundPlayer, PresenceChange.JOINED);
                }
            });
            previousPlayers.forEach((uuid, player) -> {
                if (!currentPlayers.containsKey(uuid) && config.findMatch(player.name(), scope) != null) {
                    Component styledName = formatPlayerName(player.name(), player.serverDisplayName(), config, scope);
                    messageSender.accept(createNotification(PresenceChange.LEFT, styledName, config.compactNotifications));
                    playNotificationSound(config, soundPlayer, PresenceChange.LEFT);
                }
            });
        }
        previousPlayers.clear();
        previousPlayers.putAll(currentPlayers);
    }

    static Component createNotification(PresenceChange change, Component styledName, boolean compact) {
        MutableComponent message = Component.empty().append(change == PresenceChange.JOINED ? JOIN_ICON : LEAVE_ICON);
        if (compact) {
            return message.append(styledName);
        }
        String key = change == PresenceChange.JOINED
                ? "tabnamedimmer.notification.joined"
                : "tabnamedimmer.notification.left";
        return message.append(Component.translatable(key, styledName));
    }

    private static void playNotificationSound(TabNameDimmerConfig config, Consumer<PresenceChange> soundPlayer,
                                              PresenceChange change) {
        if (config.notificationSoundsEnabled) {
            soundPlayer.accept(change);
        }
    }

    public static Component formatPlayerName(String name, Component serverDisplayName, TabNameDimmerConfig config, String scope) {
        String safeName = name == null ? "" : name;
        Component base = serverDisplayName != null ? serverDisplayName.copy() : Component.literal(safeName);
        TabNameDimmerConfig.Match match = config.findMatch(safeName, scope);
        if (match != null && match.group().colorizeNames) {
            return applyColor(base, match.group().color);
        }
        return base;
    }

    private static Component applyColor(Component original, int color) {
        if (original == null) {
            return Component.empty();
        }
        if (original.getSiblings().isEmpty()) {
            return original.copy().withStyle(style -> style.withColor(color));
        }
        MutableComponent styled = Component.empty();
        for (Component segment : original.toFlatList()) {
            styled.append(segment.copy().withStyle(style -> style.withColor(color)));
        }
        return styled;
    }

    public void clear() {
        previousPlayers.clear();
        previousScope = "";
        ticks = 0;
        initialized = false;
    }
}
