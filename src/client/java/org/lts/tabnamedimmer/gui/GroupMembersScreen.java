package org.lts.tabnamedimmer.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lts.tabnamedimmer.TabNameDimmerClient;
import org.lts.tabnamedimmer.TabNameDimmerConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

final class GroupMembersScreen extends Screen {
    private static final int ROW_HEIGHT = 26;
    private final Screen parent;
    private final TabNameDimmerConfig config;
    private final TabNameDimmerConfig.Profile profile;
    private final TabNameDimmerConfig.PlayerGroup group;
    private MemberList memberList;
    private String filter = "";
    private String status = "";
    private boolean profileImported;
    private boolean clearArmed;

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
            ScreenNavigator.show(minecraft, new OnlinePlayersScreen(this, config, profile, group));
        }).bounds(left, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.import_txt"), button -> importText())
                .bounds(left + (buttonWidth + gap), y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.export_profile"), button -> exportProfile())
                .bounds(left, y + 24, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.import_profile"), button -> importProfile())
                .bounds(left + buttonWidth + gap, y + 24, buttonWidth, 20).build());

        int quarterWidth = (buttonWidth - gap) / 2;
        EditBox filterField = new EditBox(font, left, y + 48, buttonWidth, 20,
                Component.translatable("tabnamedimmer.field.member_filter"));
        filterField.setMaxLength(64);
        filterField.setHint(Component.translatable("tabnamedimmer.field.member_filter"));
        filterField.setValue(filter);
        filterField.setResponder(value -> {
            filter = value;
            if (memberList != null) {
                memberList.setFilter(value);
            }
        });
        addRenderableWidget(filterField);
        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.sort_members"), button -> {
            clearArmed = false;
            status = "";
            memberList.sortNames(config.caseSensitive);
            rebuildWidgets();
        }).bounds(left + buttonWidth + gap, y + 48, quarterWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.sort_members"))).build());
        addRenderableWidget(Button.builder(Component.translatable(clearArmed
                ? "tabnamedimmer.button.confirm" : "tabnamedimmer.button.clear_members"), button -> {
            saveMembers();
            status = "";
            if (!clearArmed) {
                clearArmed = true;
            } else {
                clearArmed = false;
                group.members = new ArrayList<>();
            }
            rebuildWidgets();
        }).bounds(left + buttonWidth + gap * 2 + quarterWidth, y + 48, buttonWidth - quarterWidth - gap, 20).build());

        int listTop = y + 74;
        int listHeight = Math.max(20, height - listTop - 42);
        memberList = new MemberList(left, listTop, contentWidth, listHeight);
        memberList.setFilter(filter);
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

        graphics.fill(0, 0, width, 28, 0xFF182638);
        graphics.fill(0, 27, width, 28, 0xFF2E435E);
        graphics.centeredText(font, title, width / 2, 9, 0xFFFFFFFF);
        Component line = status.isBlank()
                ? Component.translatable("tabnamedimmer.members.count", memberList == null ? 0 : memberList.count())
                : Component.literal(status);
        graphics.centeredText(font, line, width / 2, 29, 0xFF88C0D0);
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_S && event.hasControlDown()) {
            saveMembers();
            status = Component.translatable(TabNameDimmerConfig.save(config)
                    ? "tabnamedimmer.toast.saved.description"
                    : "tabnamedimmer.toast.save_failed").getString();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void resize(int width, int height) {
        saveMembers();
        super.resize(width, height);
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
        openDialog("tabnamedimmer.import.title", false, this::importTextFrom);
    }

    private void importTextFrom(String selected) {
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
            saveMembers();
            status = Component.translatable("tabnamedimmer.import.added", added).getString();
        } catch (IOException | RuntimeException exception) {
            TabNameDimmerClient.LOGGER.warn("Failed to import the selected name list", exception);
            status = Component.translatable("tabnamedimmer.import.failed").getString();
        }
    }

    private void exportProfile() {
        saveMembers();
        saveDialog(this::exportProfileTo);
    }

    private void exportProfileTo(String selected) {
        if (selected == null) {
            return;
        }
        Path path = Path.of(selected.toLowerCase(Locale.ROOT).endsWith(".json") ? selected : selected + ".json");
        status = Component.translatable(TabNameDimmerConfig.exportProfile(profile, path)
                ? "tabnamedimmer.export.success" : "tabnamedimmer.export.failed").getString();
    }

    private void importProfile() {
        openDialog("tabnamedimmer.import_profile.title", false, this::importProfileFrom);
    }

    private void importProfileFrom(String selected) {
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
        if (minecraft != null && minecraft.gui.screen() == this) {
            ScreenNavigator.show(minecraft, parent);
        }
    }

    private void openDialog(String titleKey, boolean multiple, Consumer<String> onSelected) {
        try {
            FileDialogs.openFile(minecraft, multiple, onSelected);
        } catch (LinkageError | RuntimeException exception) {
            TabNameDimmerClient.LOGGER.warn("Unable to open a file dialog for {}", titleKey, exception);
            status = Component.translatable("tabnamedimmer.import.no_dialog").getString();
        }
    }

    private void saveDialog(Consumer<String> onSelected) {
        try {
            FileDialogs.saveFile(minecraft, "tabnamedimmer-profile.json", onSelected);
        } catch (LinkageError | RuntimeException exception) {
            TabNameDimmerClient.LOGGER.warn("Unable to open a save dialog", exception);
            status = Component.translatable("tabnamedimmer.import.no_dialog").getString();
        }
    }

    private final class MemberList extends ContainerObjectSelectionList<MemberEntry> {
        private final int left;
        private final int rowWidth;
        private final List<MemberEntry> all = new ArrayList<>();
        private String filterKey = "";

        MemberList(int left, int top, int width, int height) {
            super(GroupMembersScreen.this.minecraft, width, height, top, ROW_HEIGHT);
            this.left = left;
            this.rowWidth = width;
            setX(left);
            setWidth(width);
            setHeight(height);
        }

        void addName(String name) {
            MemberEntry entry = new MemberEntry(this, name);
            all.add(entry);
            if (visible(entry)) {
                addEntry(entry);
            }
        }

        void setFilter(String value) {
            String key = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            if (!key.equals(filterKey)) {
                filterKey = key;
                replaceEntries(all.stream().filter(this::visible).toList());
                setScrollAmount(0.0D);
            }
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
            replaceNames(merged.values());
            return merged.size() - before;
        }

        void sortNames(boolean caseSensitive) {
            Map<String, String> unique = new LinkedHashMap<>();
            for (String name : names()) {
                unique.putIfAbsent(key(name, caseSensitive), name);
            }
            List<String> sorted = new ArrayList<>(unique.values());
            sorted.sort(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()));
            group.members = sorted;
        }

        private void replaceNames(Collection<String> names) {
            all.clear();
            clearEntries();
            names.forEach(this::addName);
            ensureTrailingEmptyRow();
        }

        void ensureTrailingEmptyRow() {
            if (all.size() <= TabNameDimmerConfig.MAX_MEMBERS_PER_GROUP
                    && (all.isEmpty() || !all.getLast().value().isBlank())) {
                addName("");
            }
        }

        int count() {
            int count = 0;
            for (MemberEntry entry : all) {
                if (!entry.value().isBlank()) {
                    count++;
                }
            }
            return count;
        }

        List<String> names() {
            List<String> names = new ArrayList<>();
            for (MemberEntry entry : all) {
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

        private boolean visible(MemberEntry entry) {
            String value = entry.value();
            return filterKey.isEmpty() || value.isBlank() || value.toLowerCase(Locale.ROOT).contains(filterKey);
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
