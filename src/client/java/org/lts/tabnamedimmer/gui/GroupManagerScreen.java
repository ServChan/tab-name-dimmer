package org.lts.tabnamedimmer.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lts.tabnamedimmer.TabNameDimmerConfig;


final class GroupManagerScreen extends Screen {
    private final Screen parent;
    private final TabNameDimmerConfig config;
    private final String serverScope;
    private boolean serverProfile;
    private int groupIndex;
    private EditBox groupName;
    private EditBox groupColor;
    private Button groupButton;
    private Button enabledButton;
    private Button colorizeButton;
    private Button glowButton;
    private Button transparencyButton;
    private Button priorityButton;
    private Button deleteProfileButton;

    GroupManagerScreen(Screen parent, TabNameDimmerConfig config, String serverScope) {
        super(Component.translatable("tabnamedimmer.screen.groups"));
        this.parent = parent;
        this.config = config;
        this.serverScope = serverScope;
        this.serverProfile = serverScope != null
                && !TabNameDimmerConfig.GLOBAL_SCOPE.equals(serverScope)
                && config.serverProfiles.containsKey(serverScope);
    }

    @Override
    protected void init() {
        TabNameDimmerConfig.Profile profile = profile();
        groupIndex = Math.max(0, Math.min(groupIndex, profile.groups.size() - 1));
        int contentWidth = Math.max(280, Math.min(520, width - 40));
        int left = (width - contentWidth) / 2;
        int gap = 8;
        int columnWidth = (contentWidth - gap) / 2;
        int y = 40;

        Button scopeButton = Button.builder(scopeLabel(), button -> {
            if (!hasServerScope()) {
                return;
            }
            saveFields();
            serverProfile = !serverProfile;
            if (serverProfile) {
                config.getOrCreateServerProfile(serverScope);
            }
            groupIndex = 0;
            rebuildWidgets();
        }).bounds(left, y, columnWidth, 20).build();
        scopeButton.active = hasServerScope();
        addRenderableWidget(scopeButton);
        deleteProfileButton = addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.delete_server_profile"), button -> {
            if (serverProfile && config.removeServerProfile(serverScope)) {
                serverProfile = false;
                groupIndex = 0;
                rebuildWidgets();
            }
        }).bounds(left + columnWidth + gap, y, columnWidth, 20).build());
        deleteProfileButton.active = serverProfile;
        y += 24;

        groupButton = addRenderableWidget(Button.builder(groupLabel(), button -> {
            saveFields();
            groupIndex = (groupIndex + 1) % profile().groups.size();
            rebuildWidgets();
        }).bounds(left, y, columnWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.add_group"), button -> {
            saveFields();
            TabNameDimmerConfig.PlayerGroup group = TabNameDimmerConfig.PlayerGroup.defaultGroup();
            group.name = Component.translatable("tabnamedimmer.group.new", profile().groups.size() + 1).getString();
            group.priority = profile().groups.size();
            group.colorizeNames = true;
            profile().groups.add(group);
            groupIndex = profile().groups.size() - 1;
            rebuildWidgets();
        }).bounds(left + columnWidth + gap, y, columnWidth, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.delete_group"), button -> {
            if (profile().groups.size() > 1) {
                profile().groups.remove(groupIndex);
                groupIndex = Math.max(0, groupIndex - 1);
                rebuildWidgets();
            }
        }).bounds(left, y, columnWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.manage_members"), button -> {
            saveFields();
            ScreenNavigator.show(minecraft, new GroupMembersScreen(this, config, profile(), group()));
        }).bounds(left + columnWidth + gap, y, columnWidth, 20).build());
        y += 30;

        groupName = new EditBox(font, left, y, columnWidth, 20, Component.translatable("tabnamedimmer.field.group_name"));
        groupName.setMaxLength(64);
        groupName.setValue(group().name);
        groupName.setHint(Component.translatable("tabnamedimmer.field.group_name"));
        addRenderableWidget(groupName);
        groupColor = new EditBox(font, left + columnWidth + gap, y, columnWidth, 20,
                Component.translatable("tabnamedimmer.field.group_color"));
        groupColor.setMaxLength(7);
        groupColor.setValue(String.format("#%06X", group().color));
        groupColor.setHint(Component.translatable("tabnamedimmer.field.group_color"));
        groupColor.setResponder(value -> updateColorValidation());
        addRenderableWidget(groupColor);
        y += 26;

        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), button -> {
            group().enabled = !group().enabled;
            button.setMessage(enabledLabel());
        }).bounds(left, y, columnWidth, 20).build());
        colorizeButton = addRenderableWidget(Button.builder(colorizeLabel(), button -> {
            group().colorizeNames = !group().colorizeNames;
            button.setMessage(colorizeLabel());
        }).bounds(left + columnWidth + gap, y, columnWidth, 20).build());
        y += 24;

        glowButton = addRenderableWidget(Button.builder(glowLabel(), button -> {
            group().glowingEnabled = !group().glowingEnabled;
            button.setMessage(glowLabel());
        }).bounds(left, y, columnWidth, 20).build());
        transparencyButton = addRenderableWidget(Button.builder(groupTransparencyLabel(), button -> {
            group().transparencyEnabled = !group().transparencyEnabled;
            button.setMessage(groupTransparencyLabel());
        }).bounds(left + columnWidth + gap, y, columnWidth, 20).build());
        y += 20;

        priorityButton = addRenderableWidget(Button.builder(priorityLabel(), button -> {
            int[] values = {-10, 0, 10, 50, 100};
            int next = values[0];
            for (int value : values) {
                if (value > group().priority) {
                    next = value;
                    break;
                }
            }
            group().priority = next;
            button.setMessage(priorityLabel());
        }).bounds(left, y, contentWidth, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 100, height - 30, 200, 20).build());
        updateColorValidation();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, width, height, 0xF010141C);
        graphics.centeredText(font, title, width / 2, 14, 0xFFFFFFFF);
        graphics.centeredText(font, Component.literal(serverProfile ? serverScope : TabNameDimmerConfig.GLOBAL_SCOPE),
                width / 2, 27, 0xFF8FBCBB);
        if (groupName != null && groupColor != null) {
            graphics.text(font, Component.translatable("tabnamedimmer.field.group_name"),
                    groupName.getX(), groupName.getY() - 10, 0xFFB8C0CC);
            graphics.text(font, Component.translatable("tabnamedimmer.field.group_color"),
                    groupColor.getX(), groupColor.getY() - 10, 0xFFB8C0CC);
        }
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public void onClose() {
        saveFields();
        ScreenNavigator.show(minecraft, parent);
    }

    private void saveFields() {
        if (groupName != null) {
            String name = groupName.getValue().trim();
            if (!name.isBlank()) {
                group().name = name;
            }
        }
        Integer color = groupColor == null ? null : parseColor(groupColor.getValue());
        if (color != null) {
            group().color = color;
        }
    }

    private void updateColorValidation() {
        if (groupColor != null) {
            groupColor.setTextColor(parseColor(groupColor.getValue()) == null ? 0xFFFF5555 : 0xFFE0E0E0);
        }
    }

    private TabNameDimmerConfig.Profile profile() {
        return serverProfile ? config.getOrCreateServerProfile(serverScope) : config.globalProfile;
    }

    private TabNameDimmerConfig.PlayerGroup group() {
        return profile().groups.get(Math.max(0, Math.min(groupIndex, profile().groups.size() - 1)));
    }

    private boolean hasServerScope() {
        return serverScope != null && !TabNameDimmerConfig.GLOBAL_SCOPE.equals(serverScope);
    }

    private Component scopeLabel() {
        return Component.translatable("tabnamedimmer.option.profile",
                Component.translatable(serverProfile ? "tabnamedimmer.profile.server" : "tabnamedimmer.profile.global"));
    }

    private Component groupLabel() {
        return Component.translatable("tabnamedimmer.option.group", group().name,
                groupIndex + 1, profile().groups.size());
    }

    private Component enabledLabel() {
        return Component.translatable("tabnamedimmer.option.group_enabled", TabNameDimmerConfigScreen.onOff(group().enabled));
    }

    private Component colorizeLabel() {
        return Component.translatable("tabnamedimmer.option.group_colorize", TabNameDimmerConfigScreen.onOff(group().colorizeNames));
    }

    private Component glowLabel() {
        return Component.translatable("tabnamedimmer.option.glowing", TabNameDimmerConfigScreen.onOff(group().glowingEnabled));
    }

    private Component groupTransparencyLabel() {
        return Component.translatable("tabnamedimmer.option.group_transparency", TabNameDimmerConfigScreen.onOff(group().transparencyEnabled));
    }

    private Component priorityLabel() {
        return Component.translatable("tabnamedimmer.option.group_priority", group().priority);
    }

    private static Integer parseColor(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.matches("[0-9a-fA-F]{6}")) {
            return null;
        }
        try {
            return Integer.parseInt(normalized, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
