package org.lts.tabnamedimmer.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.lts.tabnamedimmer.TabNameDimmerClient;
import org.lts.tabnamedimmer.TabNameDimmerConfig;
import org.lts.tabnamedimmer.LineOfSightCache;
import org.lts.tabnamedimmer.ServerScopeTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowingMixin {

    @org.spongepowered.asm.mixin.Unique
    private TabNameDimmerConfig.Match tabNameDimmer$glowingMatch(Player player, TabNameDimmerConfig config) {
        TabNameDimmerConfig.Match match = config.findMatch(player.getGameProfile().name(), ServerScopeTracker.currentScope());
        if (!config.enabled || match == null || !match.group().glowingEnabled) {
            return null;
        }
        if (player.isSpectator() || player.isInvisible() || player.isCrouching()) {
            return null;
        }
        return LineOfSightCache.INSTANCE.canSee(player) ? match : null;
    }

    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void tabNameDimmer$onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && (Object) this instanceof Player player) {
            if (TabNameDimmerClient.isActivationActive() && TabNameDimmerClient.isTabListOpen()) {
                TabNameDimmerConfig config = TabNameDimmerConfig.current();
                if (tabNameDimmer$glowingMatch(player, config) != null) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void tabNameDimmer$onGetTeamColor(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Player player) {
            if (TabNameDimmerClient.isActivationActive() && TabNameDimmerClient.isTabListOpen()) {
                TabNameDimmerConfig config = TabNameDimmerConfig.current();
                TabNameDimmerConfig.Match match = tabNameDimmer$glowingMatch(player, config);
                if (match != null) {
                    cir.setReturnValue(match.group().color);
                }
            }
        }
    }

}
