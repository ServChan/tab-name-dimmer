package org.lts.tabnamedimmer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.lts.tabnamedimmer.TabStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Comparator;
import java.util.stream.Stream;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlaySortingMixin {
    @WrapOperation(method = "getPlayerInfos", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"))
    private Stream<PlayerInfo> tabnamedimmer$customSort(Stream<PlayerInfo> stream, Comparator<? super PlayerInfo> originalComparator, Operation<Stream<PlayerInfo>> original) {
        Stream<PlayerInfo> originallySorted = original.call(stream, originalComparator);

        return TabStateTracker.INSTANCE.processPlayers(originallySorted, (left, right) -> 0);
    }
}
