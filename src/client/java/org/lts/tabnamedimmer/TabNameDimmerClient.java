package org.lts.tabnamedimmer;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabNameDimmerClient implements ClientModInitializer {
    public static final String MOD_ID = "tabnamedimmer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "main"));
    private static KeyMapping activationKey;
    private static volatile boolean tabListOpen;

    @Override
    public void onInitializeClient() {
        TabNameDimmerConfig.load();
        activationKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tabnamedimmer.activate",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_LALT,
                CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tabListOpen = client != null && client.options != null && client.options.keyPlayerList.isDown();
            TabNameDimmerConfig.pollForChanges();
            ServerScopeTracker.update(client);
            ActivationController.tick(client, activationKey);
            LineOfSightCache.INSTANCE.refresh(client);
            OnlinePlayerTracker.INSTANCE.tick(client);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PlayerTransparencyTracker.INSTANCE.clear();
            LineOfSightCache.INSTANCE.clear();
            TabStateTracker.INSTANCE.clear();
            ServerScopeTracker.clear();
            ActivationController.reset();
            OnlinePlayerTracker.INSTANCE.clear();
            tabListOpen = false;
        });
    }

    public static boolean isShiftDown() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return false;
        }

        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
    }

    public static boolean isTabListOpen() {
        return tabListOpen;
    }

    public static boolean isActivationActive() {
        return ActivationController.isActive();
    }
}
