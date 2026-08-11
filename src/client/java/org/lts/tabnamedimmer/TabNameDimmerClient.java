package org.lts.tabnamedimmer;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabNameDimmerClient implements ClientModInitializer {
    public static final String MOD_ID = "tabnamedimmer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "main"));
    private static KeyMapping activationKey;

    @Override
    public void onInitializeClient() {
        TabNameDimmerConfig.load();
        activationKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tabnamedimmer.activate",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_LALT,
                CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            TabNameDimmerConfig.pollForChanges();
            ServerScopeTracker.update(client);
            ActivationController.tick(client, activationKey);
            LineOfSightCache.INSTANCE.clear();
            OnlinePlayerTracker.INSTANCE.tick(client);
        });
        // Entity IDs are reassigned on every new connection; clear stale entries
        // so that wrong players are never made transparent after a reconnect.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PlayerTransparencyTracker.INSTANCE.clear();
            TabStateTracker.INSTANCE.clear();
            ServerScopeTracker.clear();
            ActivationController.reset();
            OnlinePlayerTracker.INSTANCE.clear();
        });
    }

    public static boolean isShiftDown() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return false;
        }

        long window = minecraft.getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    public static boolean isTabListOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return false;
        }
        return minecraft.options.keyPlayerList.isDown();
    }

    public static boolean isActivationActive() {
        return ActivationController.isActive();
    }
}
