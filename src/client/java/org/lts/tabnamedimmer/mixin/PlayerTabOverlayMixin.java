package org.lts.tabnamedimmer.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.network.chat.Component;
import org.lts.tabnamedimmer.PlayerNameStyler;
import org.lts.tabnamedimmer.TabNameDimmerClient;
import org.lts.tabnamedimmer.TabNameDimmerConfig;
import org.lts.tabnamedimmer.ServerScopeTracker;
import org.lts.tabnamedimmer.TabStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void tabnamedimmer$dimUnlistedName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> callbackInfo) {
        if (!TabNameDimmerClient.isActivationActive()) {
            return;
        }
        String playerName = playerInfo.getProfile().name();
        callbackInfo.setReturnValue(PlayerNameStyler.style(callbackInfo.getReturnValue(), playerName));
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void tabnamedimmer$renderExtraHud(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int width, net.minecraft.world.scores.Scoreboard scoreboard, net.minecraft.world.scores.Objective objective, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!TabNameDimmerClient.isActivationActive()) return;
        TabNameDimmerConfig config = TabNameDimmerConfig.current();
        if (!config.enabled) return;
        if (config.displayMode != TabNameDimmerConfig.DisplayMode.EXTRA_HUD) return;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;

        java.util.List<PlayerInfo> whitelisted = TabStateTracker.sortedTrackedPlayers(
                mc.player.connection.getListedOnlinePlayers(), config, ServerScopeTracker.currentScope());

        if (whitelisted.isEmpty()) return;

        int padding = 5;
        int avatarSize = config.hudShowAvatars ? 8 : 0;
        int avatarGap = config.hudShowAvatars ? 3 : 0;
        int guiHeight = mc.getWindow().getGuiScaledHeight();
        int columns = Math.min(config.hudColumns, whitelisted.size());
        int rowsByHeight = Math.max(1, (guiHeight - 50 - padding * 2) / 10);
        int maxEntries = Math.min(config.hudMaxRows, rowsByHeight) * columns;
        if (whitelisted.size() > maxEntries) {
            whitelisted = whitelisted.subList(0, maxEntries);
        }
        int rows = (whitelisted.size() + columns - 1) / columns;
        net.minecraft.client.gui.Font font = mc.font;
        int cellWidth = 0;
        for (PlayerInfo p : whitelisted) {
            int pingWidth = config.hudShowPing ? font.width(p.getLatency() + " ms") + 6 : 0;
            cellWidth = Math.max(cellWidth, avatarSize + avatarGap + font.width(p.getProfile().name()) + pingWidth);
        }
        int columnGap = 8;
        int boxWidth = cellWidth * columns + columnGap * (columns - 1) + padding * 2;
        int boxHeight = rows * 10 + padding * 2;
        int margin = 8;
        int x = switch (config.hudAnchor) {
            case TOP_LEFT, BOTTOM_LEFT -> margin;
            case TOP_RIGHT, BOTTOM_RIGHT -> width - boxWidth - margin;
            case BOTTOM_CENTER -> width / 2 - boxWidth / 2;
        };
        int y = switch (config.hudAnchor) {
            case TOP_LEFT, TOP_RIGHT -> margin;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> Math.max(margin, guiHeight - boxHeight - 40);
        };

        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0x80000000);
        for (int index = 0; index < whitelisted.size(); index++) {
            PlayerInfo info = whitelisted.get(index);
            int column = index / rows;
            int row = index % rows;
            int cellX = x + padding + column * (cellWidth + columnGap);
            int cellY = y + padding + row * 10;
            if (config.hudShowAvatars) {
                PlayerFaceExtractor.extractRenderState(graphics, info.getSkin(), cellX, cellY, avatarSize);
            }
            int textX = cellX + avatarSize + avatarGap;
            TabNameDimmerConfig.Match match = config.findMatch(info.getProfile().name(), ServerScopeTracker.currentScope());
            int nameColor = match == null ? 0xFFFFFF : match.group().color;
            graphics.text(font, Component.literal(info.getProfile().name()), textX, cellY, 0xFF000000 | nameColor);
            if (config.hudShowPing) {
                String ping = info.getLatency() + " ms";
                graphics.text(font, Component.literal(ping), cellX + cellWidth - font.width(ping), cellY, 0xFF8FBCBB);
            }
        }
    }
}
