package org.lts.tabnamedimmer.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lts.tabnamedimmer.TabNameDimmerConfig;

final class HudSettingsScreen extends Screen {
    private final Screen parent;
    private final TabNameDimmerConfig config;

    HudSettingsScreen(Screen parent, TabNameDimmerConfig config) {
        super(Component.translatable("tabnamedimmer.screen.hud"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int width = Math.min(420, Math.max(1, this.width - 20));
        int left = (this.width - width) / 2;
        int y = 48;
        addRenderableWidget(Button.builder(anchorLabel(), button -> {
            TabNameDimmerConfig.HudAnchor[] values = TabNameDimmerConfig.HudAnchor.values();
            config.hudAnchor = values[(config.hudAnchor.ordinal() + 1) % values.length];
            button.setMessage(anchorLabel());
        }).bounds(left, y, width, 20).build());
        y += 26;
        addRenderableWidget(Button.builder(columnsLabel(), button -> {
            config.hudColumns = config.hudColumns % 4 + 1;
            button.setMessage(columnsLabel());
        }).bounds(left, y, width, 20).build());
        y += 26;
        addRenderableWidget(Button.builder(rowsLabel(), button -> {
            int[] values = {5, 10, 20, 40, 100};
            int next = values[0];
            for (int value : values) {
                if (value > config.hudMaxRows) {
                    next = value;
                    break;
                }
            }
            config.hudMaxRows = next;
            button.setMessage(rowsLabel());
        }).bounds(left, y, width, 20).build());
        y += 26;
        addRenderableWidget(Button.builder(avatarsLabel(), button -> {
            config.hudShowAvatars = !config.hudShowAvatars;
            button.setMessage(avatarsLabel());
        }).bounds(left, y, width, 20).build());
        y += 26;
        addRenderableWidget(Button.builder(pingLabel(), button -> {
            config.hudShowPing = !config.hudShowPing;
            button.setMessage(pingLabel());
        }).bounds(left, y, width, 20).build());

        int doneWidth = Math.min(200, Math.max(1, this.width - 20));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds((this.width - doneWidth) / 2, this.height - 30, doneWidth, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, width, height, 0xF00D141F);

        graphics.fill(0, 0, width, 28, 0xFF182638);
        graphics.fill(0, 27, width, 28, 0xFF2E435E);
        graphics.centeredText(font, title, width / 2, 9, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public void onClose() {
        ScreenNavigator.show(minecraft, parent);
    }

    private Component anchorLabel() {
        return Component.translatable("tabnamedimmer.option.hud_anchor",
                Component.translatable("tabnamedimmer.hud_anchor." + TabNameDimmerConfigScreen.enumKey(config.hudAnchor)));
    }

    private Component columnsLabel() {
        return Component.translatable("tabnamedimmer.option.hud_columns", config.hudColumns);
    }

    private Component rowsLabel() {
        return Component.translatable("tabnamedimmer.option.hud_rows", config.hudMaxRows);
    }

    private Component avatarsLabel() {
        return Component.translatable("tabnamedimmer.option.hud_avatars", TabNameDimmerConfigScreen.onOff(config.hudShowAvatars));
    }

    private Component pingLabel() {
        return Component.translatable("tabnamedimmer.option.hud_ping", TabNameDimmerConfigScreen.onOff(config.hudShowPing));
    }
}
