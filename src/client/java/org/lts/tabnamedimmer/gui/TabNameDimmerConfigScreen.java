package org.lts.tabnamedimmer.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.lts.tabnamedimmer.TabNameDimmerConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class TabNameDimmerConfigScreen extends Screen {
    private static final int FIELD_HEIGHT = 20;
    private static final int ROW_HEIGHT = 26;
    private static final SystemToast.SystemToastId SAVE_TOAST_ID = new SystemToast.SystemToastId();

    private final Screen parent;
    private final TabNameDimmerConfig config;

    private Button enabledButton;
    private Button caseSensitiveButton;
    private Button saveButton;
    private EditBox dimColor;
    private NameList nameList;
    private String importMessage = "";

    public TabNameDimmerConfigScreen(Screen parent) {
        super(Component.translatable("tabnamedimmer.screen.title"));
        this.parent = parent;
        this.config = TabNameDimmerConfig.currentCopy();
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(520, this.width - 40);
        int left = (this.width - contentWidth) / 2;
        int columnGap = 8;
        int columnWidth = (contentWidth - columnGap) / 2;
        int y = 44;

        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), button -> {
            config.enabled = !config.enabled;
            button.setMessage(enabledLabel());
        }).bounds(left, y, columnWidth, FIELD_HEIGHT).build());

        addRenderableWidget(Button.builder(displayModeLabel(), button -> {
            config.displayMode = TabNameDimmerConfig.DisplayMode.values()[(config.displayMode.ordinal() + 1) % TabNameDimmerConfig.DisplayMode.values().length];
            button.setMessage(displayModeLabel());
        }).bounds(left + columnWidth + columnGap, y, columnWidth, FIELD_HEIGHT).build());

        y += 24;

        caseSensitiveButton = addRenderableWidget(Button.builder(caseSensitiveLabel(), button -> {
            config.caseSensitive = !config.caseSensitive;
            button.setMessage(caseSensitiveLabel());
        }).bounds(left, y, columnWidth, FIELD_HEIGHT).build());

        addRenderableWidget(Button.builder(glowingEnabledLabel(), button -> {
            config.glowingEnabled = !config.glowingEnabled;
            button.setMessage(glowingEnabledLabel());
        }).bounds(left + columnWidth + columnGap, y, columnWidth, FIELD_HEIGHT).build());

        y += 24;

        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.import_txt"), button -> importNamesFromTxt())
                .bounds(left + contentWidth - 130, y, 130, FIELD_HEIGHT)
                .build());

        y += 24;
        // On a high GUI scale the logical screen can be shorter than the fixed
        // header and footer. Never let the list grow into the footer: as a
        // container it would otherwise sit on top of Save/Cancel and consume
        // their mouse and keyboard events.
        int listHeight = Math.max(0, this.height - 82 - y);
        nameList = new NameList(left, y, contentWidth, listHeight);
        if (listHeight > 0) {
            addRenderableWidget(nameList);
        }
        for (String name : config.allowedNames) {
            nameList.addName(name);
        }
        nameList.ensureTrailingEmptyRow();

        y = this.height - 58;
        dimColor = new EditBox(this.font, left, y, 120, FIELD_HEIGHT, Component.translatable("tabnamedimmer.option.dim_color"));
        dimColor.setValue(String.format("#%06X", config.dimColor));
        dimColor.setMaxLength(7);
        dimColor.setResponder(value -> updateColorValidation());
        addRenderableWidget(dimColor);

        int buttonY = this.height - 30;
        saveButton = addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.save"), button -> saveAndClose())
                .bounds(this.width / 2 - 155, buttonY, 150, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.cancel"), button -> closeScreen())
                .bounds(this.width / 2 + 5, buttonY, 150, 20)
                .build());
        updateColorValidation();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, this.width, this.height, 0xF010141C);
        graphics.centeredText(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);

        int contentWidth = Math.min(520, this.width - 40);
        int left = (this.width - contentWidth) / 2;
        int labelY = 98;
        int nameCount = nameList == null ? 0 : nameList.names().size();
        graphics.text(this.font, Component.translatable("tabnamedimmer.field.allowed_names_count", nameCount), left, labelY, 0xFFD8DEE9);
        if (!importMessage.isBlank()) {
            int labelWidth = this.font.width(Component.translatable("tabnamedimmer.field.allowed_names"));
            graphics.text(this.font, Component.literal(importMessage), left + labelWidth + 10, labelY, 0xFF88C0D0);
        }
        graphics.text(this.font, Component.translatable("tabnamedimmer.option.dim_color"), left, this.height - 72, 0xFFD8DEE9);
        Integer previewColor = parseColor(dimColor == null ? "" : dimColor.getValue());
        int swatchColor = previewColor == null ? 0xFF552222 : 0xFF000000 | previewColor;
        graphics.fill(left + 126, this.height - 60, left + 146, this.height - 42, 0xFFFFFFFF);
        graphics.fill(left + 128, this.height - 58, left + 144, this.height - 44, swatchColor);
        graphics.text(this.font, Component.translatable(previewColor == null
                ? "tabnamedimmer.color.invalid"
                : "tabnamedimmer.color.preview"), left + 154, this.height - 56,
                previewColor == null ? 0xFFFF5555 : 0xFFB8C0CC);

        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public void onClose() {
        closeScreen();
    }

    private void closeScreen() {
        // Returning to Mod Menu can be intercepted by screen-management mods
        // and reopen this config screen. When invoked in-game, close straight
        // back to the game instead of relying on that parent screen.
        this.minecraft.setScreen(this.minecraft.level == null ? parent : null);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Handle Escape before the focused list/edit box can consume it.
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_S && event.hasControlDown()) {
            saveAndClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private void saveAndClose() {
        Integer parsedColor = parseColor(dimColor.getValue());
        if (parsedColor == null) {
            updateColorValidation();
            return;
        }
        config.allowedNames = nameList.names();
        config.dimColor = parsedColor;
        TabNameDimmerConfig.save(config);
        SystemToast.addOrUpdate(
                this.minecraft.getToastManager(),
                SAVE_TOAST_ID,
                Component.translatable("tabnamedimmer.toast.saved.title"),
                Component.translatable("tabnamedimmer.toast.saved.description")
        );
        closeScreen();
    }

    private Component enabledLabel() {
        return Component.translatable("tabnamedimmer.option.enabled", onOff(config.enabled));
    }

    private Component displayModeLabel() {
        String key = switch (config.displayMode) {
            case ANIMATED_SORT -> "tabnamedimmer.mode.animated_sort";
            case FILTER -> "tabnamedimmer.mode.filter";
            case EXTRA_HUD -> "tabnamedimmer.mode.extra_hud";
        };
        return Component.translatable("tabnamedimmer.option.mode", Component.translatable(key));
    }

    private Component caseSensitiveLabel() {
        return Component.translatable("tabnamedimmer.option.case_sensitive", onOff(config.caseSensitive));
    }

    private Component glowingEnabledLabel() {
        return Component.translatable("tabnamedimmer.option.glowing", onOff(config.glowingEnabled));
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "tabnamedimmer.state.on" : "tabnamedimmer.state.off");
    }

    private void updateColorValidation() {
        boolean valid = parseColor(dimColor == null ? "" : dimColor.getValue()) != null;
        if (dimColor != null) {
            dimColor.setTextColor(valid ? 0xFFE0E0E0 : 0xFFFF5555);
        }
        if (saveButton != null) {
            saveButton.active = valid;
        }
    }

    private static Integer parseColor(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        try {
            if (!normalized.matches("[0-9a-fA-F]{6}")) {
                return null;
            }
            return Integer.parseInt(normalized, 16) & 0xFFFFFF;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void importNamesFromTxt() {
        String selectedFile = TinyFileDialogs.tinyfd_openFileDialog(
                Component.translatable("tabnamedimmer.import.title").getString(),
                "",
                null,
                null,
                false
        );

        if (selectedFile == null || selectedFile.isBlank()) {
            return;
        }
        if (!selectedFile.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            importMessage = Component.translatable("tabnamedimmer.import.not_txt").getString();
            return;
        }

        try {
            List<String> imported = Files.readAllLines(Path.of(selectedFile), StandardCharsets.UTF_8);
            int added = nameList.addNames(imported, config.caseSensitive);
            importMessage = Component.translatable("tabnamedimmer.import.added", added).getString();
        } catch (IOException | RuntimeException exception) {
            importMessage = Component.translatable("tabnamedimmer.import.failed").getString();
        }
    }

    private class NameList extends ContainerObjectSelectionList<NameEntry> {
        private final int left;
        private final int rowWidth;

        NameList(int left, int top, int width, int height) {
            super(TabNameDimmerConfigScreen.this.minecraft, width, height, top, ROW_HEIGHT);
            this.left = left;
            this.rowWidth = width;
            setX(left);
            setWidth(width);
            setHeight(height);
        }

        void addName(String name) {
            addEntry(new NameEntry(this, name));
        }

        int addNames(List<String> importedNames, boolean caseSensitive) {
            Map<String, String> merged = new LinkedHashMap<>();
            for (String name : names()) {
                merged.put(dedupeKey(name, caseSensitive), name);
            }

            int added = 0;
            for (String importedName : importedNames) {
                String name = cleanImportedName(importedName);
                if (name.isBlank()) {
                    continue;
                }
                String key = dedupeKey(name, caseSensitive);
                if (!merged.containsKey(key)) {
                    merged.put(key, name);
                    added++;
                }
            }

            clearEntries();
            for (String name : merged.values()) {
                addName(name);
            }
            ensureTrailingEmptyRow();
            return added;
        }

        void ensureTrailingEmptyRow() {
            if (children().isEmpty() || !children().get(children().size() - 1).value().isBlank()) {
                addName("");
            }
        }

        List<String> names() {
            List<String> names = new ArrayList<>();
            for (NameEntry entry : children()) {
                String name = entry.value().trim();
                if (!name.isBlank()) {
                    names.add(name);
                }
            }
            return names;
        }

        @Override
        public int getRowLeft() {
            return left;
        }

        @Override
        public int getRowRight() {
            return left + rowWidth;
        }

        @Override
        public int getRowWidth() {
            return rowWidth;
        }

        private String cleanImportedName(String name) {
            String cleaned = name == null ? "" : name.strip();
            if (!cleaned.isEmpty() && cleaned.charAt(0) == '\uFEFF') {
                cleaned = cleaned.substring(1).strip();
            }
            return cleaned;
        }

        private String dedupeKey(String name, boolean caseSensitive) {
            String trimmed = name.trim();
            return caseSensitive ? trimmed : trimmed.toLowerCase(Locale.ROOT);
        }
    }

    private class NameEntry extends ContainerObjectSelectionList.Entry<NameEntry> {
        private final NameList list;
        private final EditBox nameField;

        NameEntry(NameList list, String name) {
            this.list = list;
            this.nameField = new EditBox(TabNameDimmerConfigScreen.this.font, 0, 0, list.rowWidth - 12, FIELD_HEIGHT,
                    Component.translatable("tabnamedimmer.hint.allowed_names"));
            this.nameField.setValue(name);
            this.nameField.setMaxLength(64);
            this.nameField.setHint(Component.translatable("tabnamedimmer.hint.allowed_names"));
            this.nameField.setResponder(value -> list.ensureTrailingEmptyRow());
        }

        String value() {
            return nameField.getValue();
        }

        @Override
        public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return List.of(nameField);
        }

        @Override
        public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return List.of(nameField);
        }

        @Override
        public void visitWidgets(java.util.function.Consumer<net.minecraft.client.gui.components.AbstractWidget> consumer) {
            consumer.accept(nameField);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            nameField.setX(getContentX() + 4);
            nameField.setY(getContentY() + 3);
            nameField.setWidth(getContentWidth() - 8);
            nameField.extractWidgetRenderState(graphics, mouseX, mouseY, tickDelta);
        }
    }
}
