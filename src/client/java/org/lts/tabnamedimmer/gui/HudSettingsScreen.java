package org.lts.tabnamedimmer.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lts.tabnamedimmer.TabNameDimmerConfig;

import java.util.List;

final class HudSettingsScreen extends FramedScreen {
    private static final List<Integer> COLUMNS = List.of(1, 2, 3, 4);
    private static final List<Integer> ROWS = List.of(5, 10, 20, 40, 100);

    private final Screen parent;
    private final TabNameDimmerConfig config;

    HudSettingsScreen(Screen parent, TabNameDimmerConfig config) {
        super(Component.translatable("tabnamedimmer.screen.hud"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        OptionList list = new OptionList(minecraft, 310);
        list.place(0, HEADER, width, height - HEADER - FOOTER);
        list.cycle(Component.translatable("tabnamedimmer.option.hud_anchor"), null, List.of(TabNameDimmerConfig.HudAnchor.values()),
                anchor -> Component.translatable("tabnamedimmer.hud_anchor." + TabNameDimmerConfigScreen.enumKey(anchor)),
                () -> config.hudAnchor, value -> config.hudAnchor = value, null);
        list.cycle(Component.translatable("tabnamedimmer.option.hud_columns"), null, COLUMNS, value -> Component.literal(Integer.toString(value)),
                () -> config.hudColumns, value -> config.hudColumns = value, null);
        list.cycle(Component.translatable("tabnamedimmer.option.hud_rows"), null, ROWS, value -> Component.literal(Integer.toString(value)),
                () -> config.hudMaxRows, value -> config.hudMaxRows = value, null);
        list.toggle(Component.translatable("tabnamedimmer.option.hud_avatars"), null,
                () -> config.hudShowAvatars, value -> config.hudShowAvatars = value, null);
        list.toggle(Component.translatable("tabnamedimmer.option.hud_ping"), null,
                () -> config.hudShowPing, value -> config.hudShowPing = value, null);
        list.refresh();
        addRenderableWidget(list);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 100, footerButtonsY(), 200, 20).build());
    }

    @Override
    public void onClose() {
        ScreenNavigator.show(minecraft, parent);
    }
}
