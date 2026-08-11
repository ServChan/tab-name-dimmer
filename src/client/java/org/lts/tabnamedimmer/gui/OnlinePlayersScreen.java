package org.lts.tabnamedimmer.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.lts.tabnamedimmer.TabNameDimmerConfig;

import java.util.List;
import java.util.Locale;

final class OnlinePlayersScreen extends Screen {
    private final Screen parent;
    private final TabNameDimmerConfig config;
    private final TabNameDimmerConfig.PlayerGroup group;

    OnlinePlayersScreen(Screen parent, TabNameDimmerConfig config, TabNameDimmerConfig.PlayerGroup group) {
        super(Component.translatable("tabnamedimmer.screen.online", group.name));
        this.parent = parent;
        this.config = config;
        this.group = group;
    }

    @Override
    protected void init() {
        int contentWidth = Math.max(280, Math.min(520, width - 40));
        int left = (width - contentWidth) / 2;
        int top = 38;
        OnlineList list = new OnlineList(left, top, contentWidth, Math.max(30, height - top - 42));
        addRenderableWidget(list);
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().getListedOnlinePlayers().stream()
                    .sorted(java.util.Comparator.comparing(info -> info.getProfile().name(), String.CASE_INSENSITIVE_ORDER))
                    .forEach(info -> list.addEntryPublic(new OnlineEntry(info, contentWidth)));
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 100, height - 30, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, width, height, 0xF010141C);
        graphics.centeredText(font, title, width / 2, 14, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public void onClose() {
        ScreenNavigator.show(minecraft, parent);
    }

    private boolean contains(String name) {
        String key = normalize(name);
        return group.members.stream().map(this::normalize).anyMatch(key::equals);
    }

    private String normalize(String name) {
        String trimmed = name == null ? "" : name.trim();
        return config.caseSensitive ? trimmed : trimmed.toLowerCase(Locale.ROOT);
    }

    private final class OnlineList extends ContainerObjectSelectionList<OnlineEntry> {
        private final int left;
        private final int rowWidth;

        OnlineList(int left, int top, int width, int height) {
            super(OnlinePlayersScreen.this.minecraft, width, height, top, 26);
            this.left = left;
            this.rowWidth = width;
            setX(left);
            setWidth(width);
            setHeight(height);
        }

        void addEntryPublic(OnlineEntry entry) { addEntry(entry); }
        @Override public int getRowLeft() { return left; }
        @Override public int getRowRight() { return left + rowWidth; }
        @Override public int getRowWidth() { return rowWidth; }
    }

    private final class OnlineEntry extends ContainerObjectSelectionList.Entry<OnlineEntry> {
        private final PlayerInfo info;
        private final Button toggle;
        private final int rowWidth;

        OnlineEntry(PlayerInfo info, int rowWidth) {
            this.info = info;
            this.rowWidth = rowWidth;
            toggle = Button.builder(toggleLabel(), button -> {
                String name = info.getProfile().name();
                if (contains(name)) {
                    String key = normalize(name);
                    group.members.removeIf(member -> normalize(member).equals(key));
                } else {
                    group.members.add(name);
                }
                button.setMessage(toggleLabel());
            }).bounds(0, 0, 82, 20).build();
        }

        private Component toggleLabel() {
            return Component.translatable(contains(info.getProfile().name())
                    ? "tabnamedimmer.button.remove_player" : "tabnamedimmer.button.add_player",
                    info.getProfile().name());
        }

        @Override public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() { return List.of(toggle); }
        @Override public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() { return List.of(toggle); }
        @Override public void visitWidgets(java.util.function.Consumer<net.minecraft.client.gui.components.AbstractWidget> consumer) { consumer.accept(toggle); }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = getContentX() + 6;
            int y = getContentY() + 4;
            graphics.text(font, Component.literal(info.getProfile().name()), x, y + 2, 0xFFFFFFFF);
            String ping = info.getLatency() + " ms";
            graphics.text(font, Component.literal(ping), getContentX() + rowWidth - 150, y + 2, 0xFF8FBCBB);
            toggle.setX(getContentX() + rowWidth - 88);
            toggle.setY(y);
            toggle.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        }
    }
}
