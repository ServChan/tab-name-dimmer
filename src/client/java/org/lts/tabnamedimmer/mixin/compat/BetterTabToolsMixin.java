package org.lts.tabnamedimmer.mixin.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.lts.tabnamedimmer.PlayerNameStyler;
import org.lts.tabnamedimmer.TabNameDimmerClient;
import org.lts.tabnamedimmer.TabStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Pseudo
@Mixin(targets = "tab.bettertab.Tools", remap = false)
public class BetterTabToolsMixin {

    @Inject(method = "getPlayerEntries", at = @At("RETURN"), cancellable = true)
    private static void tabNameDimmer$onGetBetterTabEntries(Minecraft client, boolean ENABLE_MOD, boolean USE_EXAMPLES, int EXAMPLE_AMOUNT, String EXAMPLE_TEXT, Comparator<PlayerInfo> ENTRY_ORDERING, CallbackInfoReturnable<List<PlayerInfo>> cir) {
        if (ENABLE_MOD) {
            List<PlayerInfo> originalList = cir.getReturnValue();
            // Pass a comparator that does nothing, because BetterTab already sorted it.
            Stream<PlayerInfo> processed = TabStateTracker.INSTANCE.processPlayers(originalList.stream(), (a, b) -> 0);
            cir.setReturnValue(processed.toList());
        }
    }

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private static void tabNameDimmer$onGetPlayerName(PlayerInfo entry, CallbackInfoReturnable<Component> cir) {
        if (!TabNameDimmerClient.isActivationActive()) return;
        cir.setReturnValue(PlayerNameStyler.style(cir.getReturnValue(), entry.getProfile().name()));
    }
}
