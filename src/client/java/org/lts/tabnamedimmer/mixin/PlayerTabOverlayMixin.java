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
}
