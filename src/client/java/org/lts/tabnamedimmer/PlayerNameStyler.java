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
        Integer color = resolveColor(config, match);
        if (color == null) {
            return original;
        }
        MutableComponent styled = Component.empty();
        for (Component segment : original.toFlatList()) {
            styled.append(segment.copy().withStyle(style -> style.withColor(color)));
        }
        return styled;
    }

    static Integer resolveColor(TabNameDimmerConfig config, TabNameDimmerConfig.Match match) {
        if (match == null) {
            return Integer.valueOf(config.dimColor);
        }
        if (!match.group().colorizeNames) {
            return null;
        }
        return Integer.valueOf(match.group().color);
    }
}
