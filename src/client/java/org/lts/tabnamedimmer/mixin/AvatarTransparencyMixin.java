package org.lts.tabnamedimmer.mixin;

import net.minecraft.world.entity.Avatar;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.lts.tabnamedimmer.PlayerTransparencyTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarTransparencyMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("RETURN"))
    private void tabNameDimmer$capturePlayerName(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        if (avatar instanceof net.minecraft.world.entity.player.Player player) {
            PlayerTransparencyTracker.INSTANCE.put(state.id, player.getGameProfile().name());
        } else if (avatar.getProfile() != null && avatar.getProfile().name().isPresent()) {
            PlayerTransparencyTracker.INSTANCE.put(state.id, avatar.getProfile().name().get());
        }
    }
}
