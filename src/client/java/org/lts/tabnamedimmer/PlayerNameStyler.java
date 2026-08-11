package org.lts.tabnamedimmer;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class PlayerNameStyler {
    private PlayerNameStyler() {
    }

    public static Component style(Component original, String playerName) {
        if (original == null || !TabNameDimmerClient.isActivationActive()) {
            return original;
        }
        TabNameDimmerConfig config = TabNameDimmerConfig.current();
        TabNameDimmerConfig.Match match = config.findMatch(playerName, ServerScopeTracker.currentScope());
        Integer color = match == null ? config.dimColor
                : match.group().colorizeNames ? match.group().color : null;
        if (color == null) {
            return original;
        }
        MutableComponent styled = Component.empty();
        for (Component segment : original.toFlatList()) {
            styled.append(segment.copy().withStyle(style -> style.withColor(color)));
        }
        return styled;
    }
}
