package org.lts.tabnamedimmer.mixin;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.lts.tabnamedimmer.PlayerTransparencyTracker;
import org.lts.tabnamedimmer.TabNameDimmerClient;
import org.lts.tabnamedimmer.TabNameDimmerConfig;
import org.lts.tabnamedimmer.render.TranslucentSubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Wraps the SubmitNodeCollector during LivingEntityRenderer.submit for unlisted players
 * while Shift+Tab are held, applying alpha transparency to the player body AND all attached layers
 * (armor, helmets, held items, shields, totems, cape, elytra, etc.).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityTransparencyMixin {

    @ModifyVariable(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private SubmitNodeCollector tabNameDimmer$wrapCollector(SubmitNodeCollector collector, LivingEntityRenderState state) {
        if (!TabNameDimmerClient.isActivationActive() || !TabNameDimmerClient.isTabListOpen()) {
            return collector;
        }
        TabNameDimmerConfig config = TabNameDimmerConfig.current();
        if (!config.enabled) {
            return collector;
        }

        if (state instanceof AvatarRenderState avatarState) {
            String playerName = PlayerTransparencyTracker.INSTANCE.getName(avatarState.id);
            TabNameDimmerConfig.Match match = playerName == null ? null
                    : config.findMatch(playerName, org.lts.tabnamedimmer.ServerScopeTracker.currentScope());
            boolean transparent = match == null
                    ? config.playerTransparencyEnabled
                    : match.group().transparencyEnabled;
            if (playerName != null && transparent) {
                return new TranslucentSubmitNodeCollector(collector, config.dimOpacity);
            }
        }
        return collector;
    }
}
