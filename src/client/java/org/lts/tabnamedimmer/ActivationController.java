package org.lts.tabnamedimmer;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class ActivationController {
    private static boolean toggled;
    private static volatile boolean active;

    private ActivationController() {
    }

    public static void tick(Minecraft minecraft, KeyMapping activationKey) {
        TabNameDimmerConfig config = TabNameDimmerConfig.current();
        if (!config.enabled || minecraft == null || minecraft.level == null) {
            active = false;
            while (activationKey != null && activationKey.consumeClick()) {
                // Discard stale clicks from menus and loading screens.
            }
            return;
        }

        switch (config.activationMode) {
            case HOLD_SHIFT -> {
                toggled = false;
                active = TabNameDimmerClient.isShiftDown();
                while (activationKey != null && activationKey.consumeClick()) {
                    // The custom mapping is inactive in Shift mode.
                }
            }
            case HOLD_KEY -> {
                toggled = false;
                active = activationKey != null && activationKey.isDown();
                while (activationKey != null && activationKey.consumeClick()) {
                    // consumeClick is not used by hold mode.
                }
            }
            case TOGGLE_KEY -> {
                while (activationKey != null && activationKey.consumeClick()) {
                    toggled = !toggled;
                }
                active = toggled;
            }
        }
    }

    public static boolean isActive() {
        return active && TabNameDimmerConfig.current().enabled;
    }

    public static void reset() {
        toggled = false;
        active = false;
    }
}
