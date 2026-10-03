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
    private final TabNameDimmerConfig.Profile profile;
    private final TabNameDimmerConfig.PlayerGroup group;

    OnlinePlayersScreen(Screen parent, TabNameDimmerConfig config, TabNameDimmerConfig.Profile profile,
                        TabNameDimmerConfig.PlayerGroup group) {
        super(Component.translatable("tabnamedimmer.screen.online", group.name));
        this.parent = parent;
        this.config = config;
        this.profile = profile;
        this.group = group;
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(520, Math.max(1, width - 20));
        int left = (width - contentWidth) / 2;
        int top = 38;
        OnlineList list = new OnlineList(left, top, contentWidth, Math.max(30, height - top - 42));
        addRenderableWidget(list);
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().getListedOnlinePlayers().stream()
                    .filter(info -> info != null && info.getProfile() != null
                            && info.getProfile().name() != null && !info.getProfile().name().isBlank())
                    .sorted(java.util.Comparator.comparing(info -> info.getProfile().name(), String.CASE_INSENSITIVE_ORDER))
                    .forEach(info -> list.addEntryPublic(new OnlineEntry(info, contentWidth)));
        }
        int doneWidth = Math.min(200, Math.max(1, width - 20));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds((width - doneWidth) / 2, height - 30, doneWidth, 20).build());
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
        private boolean member;

        OnlineEntry(PlayerInfo info, int rowWidth) {
            this.info = info;
            this.rowWidth = rowWidth;
            this.member = contains(info.getProfile().name());
            toggle = Button.builder(toggleLabel(), button -> {
                String name = info.getProfile().name();
                if (contains(name)) {
                    String key = normalize(name);
                    group.members.removeIf(existing -> normalize(existing).equals(key));
                } else if (group.members.size() < TabNameDimmerConfig.MAX_MEMBERS_PER_GROUP) {
                    group.members.add(name);
                }
                member = contains(name);
                button.setMessage(toggleLabel());
            }).bounds(0, 0, 82, 20).build();
        }

        private Component toggleLabel() {
            return Component.translatable(member
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
            String name = info.getProfile().name();
            graphics.text(font, Component.literal(name), x, y + 2, 0xFFFFFFFF);
            TabNameDimmerConfig.Match match = profile.findMatch(name, config.caseSensitive);
            if (match != null) {
                String tagKey = match.group() != group ? "tabnamedimmer.online.in_group"
                        : member ? "tabnamedimmer.online.in_this_group" : "tabnamedimmer.online.by_mask";
                Component tag = Component.translatable(tagKey, match.group().name);
                int tagX = x + font.width(name) + 8;
                if (tagX + font.width(tag) < getContentX() + rowWidth - 154) {
                    graphics.text(font, tag, tagX, y + 2, 0xFF000000 | match.group().color);
                }
            }
            String ping = info.getLatency() + " ms";
            graphics.text(font, Component.literal(ping), getContentX() + rowWidth - 150, y + 2, 0xFF8FBCBB);
            toggle.setX(getContentX() + rowWidth - 88);
            toggle.setY(y);
            toggle.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        }
    }
}
