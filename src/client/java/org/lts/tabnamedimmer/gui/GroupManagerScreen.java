package org.lts.tabnamedimmer.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lts.tabnamedimmer.TabNameDimmerClient;
import org.lts.tabnamedimmer.TabNameDimmerConfig;

import java.util.Collections;
import java.util.List;

final class GroupManagerScreen extends Screen {
    private static final int[] PRESET_COLORS = {
            0x55FF55, 0x55FFFF, 0x5555FF, 0xFF55FF, 0xFF5555, 0xFFAA00, 0xFFFF55, 0xFFFFFF,
            0x00AA00, 0x00AAAA, 0x0000AA, 0xAA00AA, 0xAA0000, 0xAAAAAA, 0x555555
    };
    private static final String DELETE_GROUP = "group";
    private static final String DELETE_PROFILE = "profile";

    private final Screen parent;
    private final TabNameDimmerConfig config;
    private final String serverScope;
    private boolean serverProfile;
    private boolean createdServerProfile;
    private int groupIndex;
    private String armedDelete;
    private String status = "";
    private EditBox groupName;
    private EditBox groupColor;
    private Button colorSwatch;
    private int contentBottom;

    GroupManagerScreen(Screen parent, TabNameDimmerConfig config, String serverScope) {
        super(Component.translatable("tabnamedimmer.screen.groups"));
        this.parent = parent;
        this.config = config;
        this.serverScope = serverScope;
        this.serverProfile = hasServerScope() && config.hasServerProfile(serverScope);
    }

    @Override
    protected void init() {
        TabNameDimmerConfig.Profile profile = profile();
        groupIndex = Math.max(0, Math.min(groupIndex, profile.groups.size() - 1));
        int contentWidth = Math.min(520, Math.max(1, width - 20));
        int left = (width - contentWidth) / 2;
        int gap = 8;
        int columnWidth = (contentWidth - gap) / 2;
        int right = left + columnWidth + gap;
        int halfWidth = (columnWidth - 4) / 2;
        int step = height >= 280 ? 24 : 22;
        int y = height >= 280 ? 46 : 44;

        Button scopeButton = addRenderableWidget(Button.builder(scopeLabel(), button -> act(() -> {
            serverProfile = !serverProfile;
            if (serverProfile && !config.hasServerProfile(serverScope)) {
                config.getOrCreateServerProfile(serverScope);
                createdServerProfile = true;
            }
            if (!serverProfile) {
                discardUntouchedServerProfile();
            }
            groupIndex = 0;
        })).bounds(left, y, columnWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.profile"))).build());
        scopeButton.active = hasServerScope();
        Button deleteProfileButton = addRenderableWidget(Button.builder(deleteProfileLabel(), button -> {
            saveFields();
            if (!DELETE_PROFILE.equals(armedDelete)) {
                armedDelete = DELETE_PROFILE;
                rebuildWidgets();
                return;
            }
            armedDelete = null;
            if (serverProfile && config.removeServerProfile(serverScope)) {
                serverProfile = false;
                createdServerProfile = false;
                groupIndex = 0;
            }
            rebuildWidgets();
        }).bounds(right, y, columnWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.delete_server_profile"))).build());
        deleteProfileButton.active = serverProfile;
        y += step;

        Button previousButton = addRenderableWidget(Button.builder(Component.literal("<"),
                button -> act(() -> selectGroup(-1))).bounds(left, y, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.previous_group"))).build());
        addRenderableWidget(Button.builder(groupLabel(), button -> act(() -> selectGroup(1)))
                .bounds(left + 24, y, columnWidth - 48, 20).build());
        Button nextButton = addRenderableWidget(Button.builder(Component.literal(">"),
                button -> act(() -> selectGroup(1))).bounds(left + columnWidth - 20, y, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.next_group"))).build());
        previousButton.active = profile.groups.size() > 1;
        nextButton.active = profile.groups.size() > 1;
        Button addGroupButton = addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.add_group"),
                button -> act(this::addGroup)).bounds(right, y, halfWidth, 20).build());
        addGroupButton.active = profile.groups.size() < TabNameDimmerConfig.MAX_GROUPS;
        Button duplicateButton = addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.duplicate_group"),
                button -> act(this::duplicateGroup)).bounds(right + halfWidth + 4, y, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.duplicate_group"))).build());
        duplicateButton.active = profile.groups.size() < TabNameDimmerConfig.MAX_GROUPS;
        y += step;

        Button moveUpButton = addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.move_up"),
                button -> act(() -> moveGroup(-1))).bounds(left, y, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.group_order"))).build());
        moveUpButton.active = groupIndex > 0;
        Button moveDownButton = addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.move_down"),
                button -> act(() -> moveGroup(1))).bounds(left + halfWidth + 4, y, halfWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.group_order"))).build());
        moveDownButton.active = groupIndex < profile.groups.size() - 1;
        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.manage_members_count",
                group().members.size()), button -> {
            armedDelete = null;
            saveFields();
            ScreenNavigator.show(minecraft, new GroupMembersScreen(this, config, profile(), group()));
        }).bounds(right, y, halfWidth, 20).build());
        Button deleteGroupButton = addRenderableWidget(Button.builder(deleteGroupLabel(), button -> {
            saveFields();
            if (!DELETE_GROUP.equals(armedDelete)) {
                armedDelete = DELETE_GROUP;
                rebuildWidgets();
                return;
            }
            armedDelete = null;
            if (profile().groups.size() > 1) {
                profile().groups.remove(groupIndex);
                groupIndex = Math.max(0, groupIndex - 1);
            }
            rebuildWidgets();
        }).bounds(right + halfWidth + 4, y, halfWidth, 20).build());
        deleteGroupButton.active = profile.groups.size() > 1;
        y += step + 10;

        groupName = new EditBox(font, left, y, columnWidth, 20, Component.translatable("tabnamedimmer.field.group_name"));
        groupName.setMaxLength(64);
        groupName.setValue(group().name);
        groupName.setHint(Component.translatable("tabnamedimmer.field.group_name"));
        groupName.setResponder(value -> updateValidation());
        addRenderableWidget(groupName);
        groupColor = new EditBox(font, right, y, columnWidth - 24, 20,
                Component.translatable("tabnamedimmer.field.group_color"));
        groupColor.setMaxLength(7);
        groupColor.setValue(String.format("#%06X", group().color));
        groupColor.setHint(Component.translatable("tabnamedimmer.field.group_color"));
        groupColor.setResponder(value -> updateValidation());
        addRenderableWidget(groupColor);
        colorSwatch = addRenderableWidget(Button.builder(Component.empty(), button -> act(this::nextPresetColor))
                .bounds(right + columnWidth - 20, y, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.color_preset"))).build());
        y += step;

        addRenderableWidget(Button.builder(enabledLabel(), button -> act(() -> group().enabled = !group().enabled))
                .bounds(left, y, columnWidth, 20).build());
        addRenderableWidget(Button.builder(colorizeLabel(), button -> act(() -> group().colorizeNames = !group().colorizeNames))
                .bounds(right, y, columnWidth, 20).build());
        y += step;

        addRenderableWidget(Button.builder(glowLabel(), button -> act(() -> group().glowingEnabled = !group().glowingEnabled))
                .bounds(left, y, columnWidth, 20).build());
        addRenderableWidget(Button.builder(groupTransparencyLabel(),
                        button -> act(() -> group().transparencyEnabled = !group().transparencyEnabled))
                .bounds(right, y, columnWidth, 20).build());
        y += step;

        Button lowerButton = addRenderableWidget(Button.builder(Component.literal("-"), button -> act(() -> changePriority(-1)))
                .bounds(left, y, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.priority_step"))).build());
        lowerButton.active = group().priority > TabNameDimmerConfig.MIN_PRIORITY;
        addRenderableWidget(Button.builder(priorityLabel(), button -> act(() -> group().priority = 0))
                .bounds(left + 24, y, contentWidth - 48, 20)
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.priority"))).build());
        Button raiseButton = addRenderableWidget(Button.builder(Component.literal("+"), button -> act(() -> changePriority(1)))
                .bounds(left + contentWidth - 20, y, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.priority_step"))).build());
        raiseButton.active = group().priority < TabNameDimmerConfig.MAX_PRIORITY;
        contentBottom = y + 20;

        int doneWidth = Math.min(200, Math.max(1, width - 20));
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds((width - doneWidth) / 2, height - 30, doneWidth, 20).build());
        updateValidation();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, width, height, 0xF00D141F);

        graphics.fill(0, 0, width, 28, 0xFF182638);
        graphics.fill(0, 27, width, 28, 0xFF2E435E);
        graphics.centeredText(font, title, width / 2, 9, 0xFFFFFFFF);
        graphics.centeredText(font, status.isBlank() ? activeProfileLine() : Component.literal(status),
                width / 2, 32, 0xFF8FBCBB);
        if (groupName != null && groupColor != null) {
            graphics.text(font, Component.translatable("tabnamedimmer.field.group_name"),
                    groupName.getX(), groupName.getY() - 10, 0xFFB8C0CC);
            graphics.text(font, Component.translatable("tabnamedimmer.field.group_color"),
                    groupColor.getX(), groupColor.getY() - 10, 0xFFB8C0CC);
        }
        if (height - 44 > contentBottom + 4) {
            graphics.centeredText(font, Component.translatable("tabnamedimmer.groups.save_hint"),
                    width / 2, height - 44, 0xFF9AA5B4);
        }
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        if (colorSwatch != null) {
            Integer color = groupColor == null ? null : parseColor(groupColor.getValue());
            int x = colorSwatch.getX() + 4;
            int y = colorSwatch.getY() + 4;
            graphics.fill(x - 1, y - 1, x + 13, y + 13, 0xFF0D141F);
            graphics.fill(x, y, x + 12, y + 12, 0xFF000000 | (color == null ? group().color : color));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_S && event.hasControlDown()) {
            saveFields();
            status = Component.translatable(TabNameDimmerConfig.save(config)
                    ? "tabnamedimmer.toast.saved.description"
                    : "tabnamedimmer.toast.save_failed").getString();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void resize(int width, int height) {
        saveFields();
        super.resize(width, height);
    }

    @Override
    public void onClose() {
        saveFields();
        discardUntouchedServerProfile();
        ScreenNavigator.show(minecraft, parent);
    }

    private void act(Runnable action) {
        armedDelete = null;
        status = "";
        saveFields();
        action.run();
        rebuildWidgets();
    }

    private void selectGroup(int direction) {
        int size = profile().groups.size();
        groupIndex = Math.floorMod(groupIndex + direction, size);
    }

    private void addGroup() {
        List<TabNameDimmerConfig.PlayerGroup> groups = profile().groups;
        if (groups.size() >= TabNameDimmerConfig.MAX_GROUPS) {
            return;
        }
        TabNameDimmerConfig.PlayerGroup group = TabNameDimmerConfig.PlayerGroup.defaultGroup();
        group.name = Component.translatable("tabnamedimmer.group.new", groups.size() + 1).getString();
        group.priority = TabNameDimmerConfig.clampPriority(groups.size());
        group.color = PRESET_COLORS[groups.size() % PRESET_COLORS.length];
        group.colorizeNames = true;
        groups.add(group);
        groupIndex = groups.size() - 1;
    }

    private void duplicateGroup() {
        List<TabNameDimmerConfig.PlayerGroup> groups = profile().groups;
        if (groups.size() >= TabNameDimmerConfig.MAX_GROUPS) {
            return;
        }
        TabNameDimmerConfig.PlayerGroup copy = group().copy();
        String name = Component.translatable("tabnamedimmer.group.copy", copy.name).getString();
        copy.name = name.length() <= 64 ? name : name.substring(0, 64);
        groups.add(groupIndex + 1, copy);
        groupIndex++;
    }

    private void moveGroup(int direction) {
        List<TabNameDimmerConfig.PlayerGroup> groups = profile().groups;
        int target = groupIndex + direction;
        if (target < 0 || target >= groups.size()) {
            return;
        }
        Collections.swap(groups, groupIndex, target);
        groupIndex = target;
    }

    private void changePriority(int direction) {
        int amount = TabNameDimmerClient.isShiftDown() ? 10 : 1;
        group().priority = TabNameDimmerConfig.clampPriority(group().priority + direction * amount);
    }

    private void nextPresetColor() {
        int next = PRESET_COLORS[0];
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            if (PRESET_COLORS[i] == group().color) {
                next = PRESET_COLORS[(i + 1) % PRESET_COLORS.length];
                break;
            }
        }
        group().color = next;
    }

    private void discardUntouchedServerProfile() {
        if (createdServerProfile && hasServerScope() && config.hasServerProfile(serverScope)
                && config.serverProfiles.get(serverScope).sameGroupsAs(config.globalProfile)) {
            config.removeServerProfile(serverScope);
            serverProfile = false;
        }
        if (!config.hasServerProfile(serverScope)) {
            createdServerProfile = false;
        }
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

    private void updateValidation() {
        if (groupColor != null) {
            groupColor.setTextColor(parseColor(groupColor.getValue()) == null ? 0xFFFF5555 : 0xFFE0E0E0);
        }
        if (groupName != null) {
            groupName.setTextColor(groupName.getValue().isBlank() ? 0xFFFF5555 : 0xFFE0E0E0);
        }
    }

    private TabNameDimmerConfig.Profile profile() {
        return serverProfile ? config.getOrCreateServerProfile(serverScope) : config.globalProfile;
    }

    private TabNameDimmerConfig.PlayerGroup group() {
        List<TabNameDimmerConfig.PlayerGroup> groups = profile().groups;
        return groups.get(Math.max(0, Math.min(groupIndex, groups.size() - 1)));
    }

    private boolean hasServerScope() {
        return serverScope != null && !serverScope.isBlank() && !TabNameDimmerConfig.GLOBAL_SCOPE.equals(serverScope);
    }

    private Component activeProfileLine() {
        if (!hasServerScope()) {
            return Component.translatable("tabnamedimmer.groups.no_server");
        }
        return Component.translatable(config.hasServerProfile(serverScope)
                ? "tabnamedimmer.groups.active_server" : "tabnamedimmer.groups.active_global");
    }

    private Component scopeLabel() {
        return Component.translatable("tabnamedimmer.option.profile",
                Component.translatable(serverProfile ? "tabnamedimmer.profile.server" : "tabnamedimmer.profile.global"));
    }

    private Component deleteProfileLabel() {
        return Component.translatable(DELETE_PROFILE.equals(armedDelete)
                ? "tabnamedimmer.button.confirm" : "tabnamedimmer.button.delete_server_profile");
    }

    private Component deleteGroupLabel() {
        return Component.translatable(DELETE_GROUP.equals(armedDelete)
                ? "tabnamedimmer.button.confirm" : "tabnamedimmer.button.delete_group");
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

    static Integer parseColor(String value) {
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
