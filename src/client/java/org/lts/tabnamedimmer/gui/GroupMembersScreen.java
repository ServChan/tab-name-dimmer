package org.lts.tabnamedimmer.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.lts.tabnamedimmer.TabNameDimmerClient;
import org.lts.tabnamedimmer.TabNameDimmerConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class GroupMembersScreen extends Screen {
    private static final int ROW_HEIGHT = 26;
    private final Screen parent;
    private final TabNameDimmerConfig config;
    private final TabNameDimmerConfig.Profile profile;
    private final TabNameDimmerConfig.PlayerGroup group;
    private MemberList memberList;
    private String status = "";
    private boolean profileImported;

    GroupMembersScreen(Screen parent, TabNameDimmerConfig config,
                       TabNameDimmerConfig.Profile profile, TabNameDimmerConfig.PlayerGroup group) {
        super(Component.translatable("tabnamedimmer.screen.members", group.name));
        this.parent = parent;
        this.config = config;
        this.profile = profile;
        this.group = group;
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(560, Math.max(1, width - 20));
        int left = (width - contentWidth) / 2;
        int gap = 6;
        int buttonWidth = (contentWidth - gap) / 2;
        int y = 38;

        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.quick_add"), button -> {
            saveMembers();
            ScreenNavigator.show(minecraft, new OnlinePlayersScreen(this, config, group));
        }).bounds(left, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.import_txt"), button -> importText())
                .bounds(left + (buttonWidth + gap), y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.export_profile"), button -> exportProfile())
                .bounds(left, y + 24, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.import_profile"), button -> importProfile())
                .bounds(left + buttonWidth + gap, y + 24, buttonWidth, 20).build());

        int listTop = y + 54;
        int listHeight = Math.max(20, height - listTop - 42);
        memberList = new MemberList(left, listTop, contentWidth, listHeight);
        addRenderableWidget(memberList);
        for (String member : group.members) {
            memberList.addName(member);
        }
        memberList.ensureTrailingEmptyRow();

        int doneWidth = Math.min(200, Math.max(1, width - 20));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds((width - doneWidth) / 2, height - 30, doneWidth, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, width, height, 0xF00D141F);

        // Header bar
        graphics.fill(0, 0, width, 28, 0xFF182638);
        graphics.fill(0, 27, width, 28, 0xFF2E435E);
        graphics.centeredText(font, title, width / 2, 9, 0xFFFFFFFF);
        if (!status.isBlank()) {
            graphics.centeredText(font, Component.literal(status), width / 2, 21, 0xFF88C0D0);
        }
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_S && event.hasControlDown()) {
            saveMembers();
            status = Component.translatable(TabNameDimmerConfig.save(config)
                    ? "tabnamedimmer.toast.saved.description"
                    : "tabnamedimmer.toast.save_failed").getString();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (!profileImported) {
            saveMembers();
        }
        ScreenNavigator.show(minecraft, parent);
    }

    private void saveMembers() {
        if (memberList != null) {
            group.members = memberList.names();
        }
    }

    private void importText() {
        String selected = openDialog("tabnamedimmer.import.title", false);
        if (selected == null) {
            return;
        }
        Path path = Path.of(selected);
        try {
            if (!selected.toLowerCase(Locale.ROOT).endsWith(".txt")) {
                status = Component.translatable("tabnamedimmer.import.not_txt").getString();
                return;
            }
            if (Files.size(path) > TabNameDimmerConfig.MAX_TRANSFER_BYTES) {
                status = Component.translatable("tabnamedimmer.import.too_large").getString();
                return;
            }
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int added = memberList.addNames(lines, config.caseSensitive);
            status = Component.translatable("tabnamedimmer.import.added", added).getString();
        } catch (IOException | RuntimeException exception) {
            TabNameDimmerClient.LOGGER.warn("Failed to import the selected name list", exception);
            status = Component.translatable("tabnamedimmer.import.failed").getString();
        }
    }

    private void exportProfile() {
        saveMembers();
        String selected = saveDialog("tabnamedimmer.export.title");
        if (selected == null) {
            return;
        }
        Path path = Path.of(selected.toLowerCase(Locale.ROOT).endsWith(".json") ? selected : selected + ".json");
        status = Component.translatable(TabNameDimmerConfig.exportProfile(profile, path)
                ? "tabnamedimmer.export.success" : "tabnamedimmer.export.failed").getString();
    }

    private void importProfile() {
        String selected = openDialog("tabnamedimmer.import_profile.title", false);
        if (selected == null) {
            return;
        }
        TabNameDimmerConfig.Profile imported = TabNameDimmerConfig.importProfile(Path.of(selected));
        if (imported == null) {
            status = Component.translatable("tabnamedimmer.import_profile.failed").getString();
            return;
        }
        profile.name = imported.name;
        profile.groups = imported.groups;
        profileImported = true;
        ScreenNavigator.show(minecraft, parent);
    }

    private String openDialog(String titleKey, boolean multiple) {
        try {
            return TinyFileDialogs.tinyfd_openFileDialog(Component.translatable(titleKey).getString(), "",
                    null, null, multiple);
        } catch (LinkageError | RuntimeException exception) {
            TabNameDimmerClient.LOGGER.warn("Unable to open a file dialog", exception);
            status = Component.translatable("tabnamedimmer.import.no_dialog").getString();
            return null;
        }
    }

    private String saveDialog(String titleKey) {
        try {
            return TinyFileDialogs.tinyfd_saveFileDialog(Component.translatable(titleKey).getString(),
                    "tabnamedimmer-profile.json", null, null);
        } catch (LinkageError | RuntimeException exception) {
            TabNameDimmerClient.LOGGER.warn("Unable to open a save dialog", exception);
            status = Component.translatable("tabnamedimmer.import.no_dialog").getString();
            return null;
        }
    }

    private final class MemberList extends ContainerObjectSelectionList<MemberEntry> {
        private final int left;
        private final int rowWidth;

        MemberList(int left, int top, int width, int height) {
            super(GroupMembersScreen.this.minecraft, width, height, top, ROW_HEIGHT);
            this.left = left;
            this.rowWidth = width;
            setX(left);
            setWidth(width);
            setHeight(height);
        }

        void addName(String name) {
            addEntry(new MemberEntry(this, name));
        }

        int addNames(List<String> imported, boolean caseSensitive) {
            Map<String, String> merged = new LinkedHashMap<>();
            for (String name : names()) {
                merged.put(key(name, caseSensitive), name);
            }
            int before = merged.size();
            for (String line : imported) {
                for (String raw : line.split("[,;]+")) {
                    if (merged.size() >= TabNameDimmerConfig.MAX_MEMBERS_PER_GROUP) {
                        break;
                    }
                    String name = raw.strip().replace("\uFEFF", "");
                    if (!name.isBlank()) {
                        merged.putIfAbsent(key(name, caseSensitive), name);
                    }
                }
            }
            clearEntries();
            merged.values().forEach(this::addName);
            ensureTrailingEmptyRow();
            return merged.size() - before;
        }

        void ensureTrailingEmptyRow() {
            if (children().size() <= TabNameDimmerConfig.MAX_MEMBERS_PER_GROUP
                    && (children().isEmpty() || !children().getLast().value().isBlank())) {
                addName("");
            }
        }

        List<String> names() {
            List<String> names = new ArrayList<>();
            for (MemberEntry entry : children()) {
                String name = entry.value().trim();
                if (!name.isBlank()) {
                    names.add(name);
                    if (names.size() >= TabNameDimmerConfig.MAX_MEMBERS_PER_GROUP) {
                        break;
                    }
                }
            }
            return names;
        }

        @Override public int getRowLeft() { return left; }
        @Override public int getRowRight() { return left + rowWidth; }
        @Override public int getRowWidth() { return rowWidth; }

        private String key(String name, boolean caseSensitive) {
            String trimmed = name.trim();
            return caseSensitive ? trimmed : trimmed.toLowerCase(Locale.ROOT);
        }
    }

    private final class MemberEntry extends ContainerObjectSelectionList.Entry<MemberEntry> {
        private final MemberList list;
        private final EditBox field;

        MemberEntry(MemberList list, String value) {
            this.list = list;
            field = new EditBox(font, 0, 0, list.rowWidth - 12, 20,
                    Component.translatable("tabnamedimmer.hint.allowed_names"));
            field.setMaxLength(64);
            field.setValue(value);
            field.setHint(Component.translatable("tabnamedimmer.hint.allowed_names"));
            field.setResponder(ignored -> list.ensureTrailingEmptyRow());
        }

        String value() { return field.getValue(); }
        @Override public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() { return List.of(field); }
        @Override public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() { return List.of(field); }
        @Override public void visitWidgets(java.util.function.Consumer<net.minecraft.client.gui.components.AbstractWidget> consumer) { consumer.accept(field); }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            field.setX(getContentX() + 4);
            field.setY(getContentY() + 3);
            field.setWidth(getContentWidth() - 8);
            field.extractWidgetRenderState(graphics, mouseX, mouseY, tickDelta);
        }
    }
}
