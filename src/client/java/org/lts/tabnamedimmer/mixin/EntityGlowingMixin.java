package org.lts.tabnamedimmer.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.lts.tabnamedimmer.TabNameDimmerClient;
import org.lts.tabnamedimmer.TabNameDimmerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(Entity.class)
public abstract class EntityGlowingMixin {

    @org.spongepowered.asm.mixin.Unique
    private boolean tabNameDimmer$shouldGlow(Player player, TabNameDimmerConfig config) {
        if (!config.enabled || !config.glowingEnabled || config.shouldDim(player.getGameProfile().name())) {
            return false;
        }
        if (player.isSpectator() || player.isInvisible() || player.isCrouching()) {
            return false;
        }
        net.minecraft.client.player.LocalPlayer localPlayer = net.minecraft.client.Minecraft.getInstance().player;
        return localPlayer == player || (localPlayer != null && localPlayer.hasLineOfSight(player));
    }

    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void tabNameDimmer$onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && (Object) this instanceof Player player) {
            if (TabNameDimmerClient.isShiftDown() && TabNameDimmerClient.isTabListOpen()) {
                TabNameDimmerConfig config = TabNameDimmerConfig.loadIfChanged();
                if (tabNameDimmer$shouldGlow(player, config)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void tabNameDimmer$onGetTeamColor(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Player player) {
            if (TabNameDimmerClient.isShiftDown() && TabNameDimmerClient.isTabListOpen()) {
                TabNameDimmerConfig config = TabNameDimmerConfig.loadIfChanged();
                if (tabNameDimmer$shouldGlow(player, config)) {
                    int color = extractAverageColor(player);
                    if (color != -1) {
                        cir.setReturnValue(color);
                    } else {
                        cir.setReturnValue(0xFFFFFF); // default white if no color found
                    }
                }
            }
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private int tabNameDimmer$cachedColor = -2;
    
    @org.spongepowered.asm.mixin.Unique
    private long tabNameDimmer$lastColorUpdate = 0;

    private int extractAverageColor(Player player) {
        long now = System.currentTimeMillis();
        if (now - tabNameDimmer$lastColorUpdate < 5000L && tabNameDimmer$cachedColor != -2) {
            return tabNameDimmer$cachedColor;
        }
        tabNameDimmer$lastColorUpdate = now;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.getConnection() == null) {
            tabNameDimmer$cachedColor = -1;
            return -1;
        }
        net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
        if (info == null) {
            tabNameDimmer$cachedColor = -1;
            return -1;
        }

        net.minecraft.network.chat.Component displayName = info.getTabListDisplayName();
        if (displayName == null) {
            tabNameDimmer$cachedColor = -1;
            return -1;
        }

        List<Integer> colors = new ArrayList<>();
        displayName.visit((style, text) -> {
            if (style != null && style.getColor() != null) {
                colors.add(style.getColor().getValue());
            }
            return Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);

        if (colors.isEmpty()) {
            tabNameDimmer$cachedColor = -1;
            return -1;
        }

        long r = 0, g = 0, b = 0;
        for (int c : colors) {
            r += (c >> 16) & 0xFF;
            g += (c >> 8) & 0xFF;
            b += c & 0xFF;
        }
        int count = colors.size();
        tabNameDimmer$cachedColor = (int)(r / count) << 16 | (int)(g / count) << 8 | (int)(b / count);
        return tabNameDimmer$cachedColor;
    }
}
