package org.lts.tabnamedimmer.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lts.tabnamedimmer.TabNameDimmerClient;
import org.lts.tabnamedimmer.TabNameDimmerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void tabnamedimmer$dimUnlistedName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> callbackInfo) {
        if (!TabNameDimmerClient.isShiftDown()) {
            return;
        }

        TabNameDimmerConfig config = TabNameDimmerConfig.loadIfChanged();
        String playerName = playerInfo.getProfile().name();
        if (!config.shouldDim(playerName)) {
            return;
        }

        MutableComponent dimmedName = Component.empty();
        for (Component segment : callbackInfo.getReturnValue().toFlatList()) {
            dimmedName.append(segment.copy().withStyle(style -> style.withColor(config.dimColor)));
        }
        callbackInfo.setReturnValue(dimmedName);
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void tabnamedimmer$renderExtraHud(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int width, net.minecraft.world.scores.Scoreboard scoreboard, net.minecraft.world.scores.Objective objective, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!TabNameDimmerClient.isShiftDown()) return;
        TabNameDimmerConfig config = TabNameDimmerConfig.loadIfChanged();
        if (config.displayMode != TabNameDimmerConfig.DisplayMode.EXTRA_HUD) return;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;

        java.util.List<PlayerInfo> whitelisted = mc.player.connection.getListedOnlinePlayers().stream()
                .filter(p -> !config.shouldDim(p.getProfile().name()))
                .toList();

        if (whitelisted.isEmpty()) return;

        net.minecraft.client.gui.Font font = mc.font;
        int maxNameWidth = 0;
        for (PlayerInfo p : whitelisted) {
            maxNameWidth = Math.max(maxNameWidth, font.width(p.getProfile().name()));
        }

        int padding = 5;
        int boxWidth = maxNameWidth + padding * 2;
        int boxHeight = whitelisted.size() * 9 + padding * 2;
        
        int x = width / 2 - boxWidth / 2;
        int y = mc.getWindow().getGuiScaledHeight() - boxHeight - 40; // 40 pixels from bottom to avoid hotbar

        // Draw background
        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0x80000000);

        // Draw names
        int currentY = y + padding;
        for (PlayerInfo p : whitelisted) {
            graphics.text(font, net.minecraft.network.chat.Component.literal(p.getProfile().name()), x + padding, currentY, -1);
            currentY += 9;
        }
    }
}
