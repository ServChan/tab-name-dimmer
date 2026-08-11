package org.lts.tabnamedimmer.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class ScreenNavigator {
    private ScreenNavigator() {
    }

    static void show(Minecraft minecraft, Screen screen) {
        try {
            try {
                minecraft.gui.getClass().getMethod("setScreen", Screen.class).invoke(minecraft.gui, screen);
            } catch (NoSuchMethodException ignored) {
                minecraft.getClass().getMethod("setScreen", Screen.class).invoke(minecraft, screen);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to change the current screen", exception);
        }
    }
}
