package org.lts.tabnamedimmer.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lts.tabnamedimmer.TabNameDimmerClient;
import org.lts.tabnamedimmer.TabNameDimmerConfig;

import java.util.Collections;
import java.util.List;
import java.util.function.IntSupplier;

final class GroupManagerScreen extends FramedScreen {
    private static final int[] PRESET_COLORS = {
            0x55FF55, 0x55FFFF, 0x5555FF, 0xFF55FF, 0xFF5555, 0xFFAA00, 0xFFFF55, 0xFFFFFF,
            0x00AA00, 0x00AAAA, 0x0000AA, 0xAA00AA, 0xAA0000, 0xAAAAAA, 0x555555
    };
    private static final int GAP = 10;
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
    private GroupList groups;
    private OptionList details;
    private EditBox groupName;
    private EditBox groupColor;
    private int detailsX;
    private int detailsWidth;

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
        int listWidth = Mth.clamp(width * 3 / 10, 130, 180);
        detailsWidth = Math.min(width - 12 - listWidth - GAP, 330);
        int left = (width - listWidth - GAP - detailsWidth) / 2;
        detailsX = left + listWidth + GAP;
        int bodyBottom = height - FOOTER;

        CycleButton<Boolean> scope = CycleButton.booleanBuilder(Component.translatable("tabnamedimmer.profile.server"),
                        Component.translatable("tabnamedimmer.profile.global"), serverProfile)
                .displayOnlyValue()
                .withTooltip(value -> Tooltip.create(Component.translatable("tabnamedimmer.tooltip.profile")))
                .withCustomNarration(button -> CommonComponents.joinForNarration(Component.translatable("tabnamedimmer.option.profile"),
                        button.createDefaultNarrationMessage()))
                .create(left, HEADER + 4, listWidth, 20, Component.translatable("tabnamedimmer.option.profile"),
                        (button, value) -> act(() -> switchProfile(value)));
        scope.active = hasServerScope();
        addRenderableWidget(scope);

        int listTop = HEADER + 30;
        int listBottom = bodyBottom - 28;
        if (groups == null) {
            groups = new GroupList(minecraft);
        }
        groups.updateSizeAndPosition(listWidth, Math.max(20, listBottom - listTop), left, listTop);
        groups.fill(profile.groups, groupIndex);
        groups.updateSizeAndPosition(listWidth, Math.max(20, listBottom - listTop), left, listTop);
        addRenderableWidget(groups);

        Button add = addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.add_group"), button -> act(this::addGroup))
                .bounds(left, bodyBottom - 24, listWidth, 20).build());
        add.active = profile.groups.size() < TabNameDimmerConfig.MAX_GROUPS;

        buildDetails();

        int y = footerButtonsY();
        Button useGlobal = addRenderableWidget(Button.builder(deleteProfileLabel(), button -> {
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
        }).bounds(width / 2 - 155, y, 150, 20)
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.delete_server_profile"))).build());
        useGlobal.active = serverProfile;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(width / 2 + 5, y, 150, 20).build());
    }

    private void buildDetails() {
        double scroll = details == null ? 0 : details.scrollAmount();
        if (details != null) {
            removeWidget(details);
        }
        TabNameDimmerConfig.PlayerGroup group = group();
        TabNameDimmerConfig.Profile profile = profile();
        details = new OptionList(minecraft, 310);
        details.place(detailsX, HEADER, detailsWidth, height - HEADER - FOOTER);

        groupName = new EditBox(font, 0, 0, 130, 20, Component.translatable("tabnamedimmer.field.group_name"));
        groupName.setMaxLength(64);
        groupName.setValue(group.name);
        groupName.setHint(Component.translatable("tabnamedimmer.field.group_name"));
        groupName.setResponder(value -> {
            updateValidation();
            if (!value.isBlank()) {
                group.name = value.trim();
            }
        });
        details.labeled(Component.translatable("tabnamedimmer.field.group_name"), null, groupName);

        groupColor = new EditBox(font, 0, 0, 70, 20, Component.translatable("tabnamedimmer.field.group_color"));
        groupColor.setMaxLength(7);
        groupColor.setValue(String.format("#%06X", group.color));
        groupColor.setResponder(value -> {
            updateValidation();
            Integer color = parseColor(value);
            if (color != null) {
                group.color = color;
            }
        });
        SwatchButton swatch = new SwatchButton(() -> group.color, () -> {
            group.color = nextPresetColor(group.color);
            groupColor.setValue(String.format("#%06X", group.color));
        });
        details.group(Component.translatable("tabnamedimmer.field.group_color"), Component.translatable("tabnamedimmer.tooltip.color_preset"),
                List.of(groupColor, swatch));

        details.toggle(Component.translatable("tabnamedimmer.option.group_enabled"), null,
                () -> group.enabled, value -> group.enabled = value, null);
        details.toggle(Component.translatable("tabnamedimmer.option.group_colorize"), null,
                () -> group.colorizeNames, value -> group.colorizeNames = value, () -> group.enabled);
        details.toggle(Component.translatable("tabnamedimmer.option.glowing"), null,
                () -> group.glowingEnabled, value -> group.glowingEnabled = value, () -> group.enabled);
        details.toggle(Component.translatable("tabnamedimmer.option.group_transparency"), null,
                () -> group.transparencyEnabled, value -> group.transparencyEnabled = value, () -> group.enabled);

        Runnable[] sync = new Runnable[1];
        Button lower = Button.builder(Component.literal("-"), button -> {
            stepPriority(group, -1);
            sync[0].run();
        }).bounds(0, 0, 20, 20).tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.priority_step"))).build();
        Button value = Button.builder(Component.literal(Integer.toString(group.priority)), button -> {
            group.priority = 0;
            sync[0].run();
        }).bounds(0, 0, 48, 20).tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.priority"))).build();
        Button raise = Button.builder(Component.literal("+"), button -> {
            stepPriority(group, 1);
            sync[0].run();
        }).bounds(0, 0, 20, 20).tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.priority_step"))).build();
        sync[0] = () -> {
            value.setMessage(Component.literal(Integer.toString(group.priority)));
            lower.active = group.priority > TabNameDimmerConfig.MIN_PRIORITY;
            raise.active = group.priority < TabNameDimmerConfig.MAX_PRIORITY;
        };
        details.group(Component.translatable("tabnamedimmer.option.group_priority"), Component.translatable("tabnamedimmer.tooltip.priority"),
                List.of(lower, value, raise));
        sync[0].run();

        Button members = Button.builder(Component.translatable("tabnamedimmer.button.manage_members_count", group.members.size()), button -> {
            armedDelete = null;
            saveFields();
            ScreenNavigator.show(minecraft, new GroupMembersScreen(this, config, profile(), group()));
        }).build();
        Button delete = Button.builder(deleteGroupLabel(), button -> {
            saveFields();
            if (!DELETE_GROUP.equals(armedDelete)) {
                armedDelete = DELETE_GROUP;
                buildDetails();
                return;
            }
            act(() -> {
                if (profile().groups.size() > 1) {
                    profile().groups.remove(groupIndex);
                    groupIndex = Math.max(0, groupIndex - 1);
                }
            });
        }).build();
        delete.active = profile.groups.size() > 1;
        details.buttons(List.of(members, delete));

        Button up = Button.builder(Component.translatable("tabnamedimmer.button.move_up"), button -> act(() -> moveGroup(-1)))
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.group_order"))).build();
        up.active = groupIndex > 0;
        Button down = Button.builder(Component.translatable("tabnamedimmer.button.move_down"), button -> act(() -> moveGroup(1)))
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.group_order"))).build();
        down.active = groupIndex < profile.groups.size() - 1;
        Button duplicate = Button.builder(Component.translatable("tabnamedimmer.button.duplicate_group"), button -> act(this::duplicateGroup))
                .tooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.duplicate_group"))).build();
        duplicate.active = profile.groups.size() < TabNameDimmerConfig.MAX_GROUPS;
        details.buttons(List.of(up, down, duplicate));
        details.text(Component.translatable("tabnamedimmer.groups.save_hint"));

        details.refresh();
        details.setScrollAmount(scroll);
        addRenderableWidget(details);
        updateValidation();
    }

    @Override
    protected Component subtitle() {
        return status.isBlank() ? activeProfileLine() : Component.literal(status);
    }

    @Override
    protected int subtitleColor() {
        return status.isBlank() ? TEXT_DIM : NOTICE;
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

    private void select(int index) {
        if (index == groupIndex) {
            return;
        }
        saveFields();
        armedDelete = null;
        status = "";
        groupIndex = index;
        buildDetails();
    }

    private void act(Runnable action) {
        armedDelete = null;
        status = "";
        saveFields();
        action.run();
        rebuildWidgets();
    }

    private void switchProfile(boolean server) {
        serverProfile = server;
        if (serverProfile && !config.hasServerProfile(serverScope)) {
            config.getOrCreateServerProfile(serverScope);
            createdServerProfile = true;
        }
        if (!serverProfile) {
            discardUntouchedServerProfile();
        }
        groupIndex = 0;
    }

    private void addGroup() {
        List<TabNameDimmerConfig.PlayerGroup> list = profile().groups;
        if (list.size() >= TabNameDimmerConfig.MAX_GROUPS) {
            return;
        }
        TabNameDimmerConfig.PlayerGroup group = TabNameDimmerConfig.PlayerGroup.defaultGroup();
        group.name = Component.translatable("tabnamedimmer.group.new", list.size() + 1).getString();
        group.priority = TabNameDimmerConfig.clampPriority(list.size());
        group.color = PRESET_COLORS[list.size() % PRESET_COLORS.length];
        group.colorizeNames = true;
        list.add(group);
        groupIndex = list.size() - 1;
    }

    private void duplicateGroup() {
        List<TabNameDimmerConfig.PlayerGroup> list = profile().groups;
        if (list.size() >= TabNameDimmerConfig.MAX_GROUPS) {
            return;
        }
        TabNameDimmerConfig.PlayerGroup copy = group().copy();
        String name = Component.translatable("tabnamedimmer.group.copy", copy.name).getString();
        copy.name = name.length() <= 64 ? name : name.substring(0, 64);
        list.add(groupIndex + 1, copy);
        groupIndex++;
    }

    private void moveGroup(int direction) {
        List<TabNameDimmerConfig.PlayerGroup> list = profile().groups;
        int target = groupIndex + direction;
        if (target < 0 || target >= list.size()) {
            return;
        }
        Collections.swap(list, groupIndex, target);
        groupIndex = target;
    }

    private static void stepPriority(TabNameDimmerConfig.PlayerGroup group, int direction) {
        int amount = TabNameDimmerClient.isShiftDown() ? 10 : 1;
        group.priority = TabNameDimmerConfig.clampPriority(group.priority + direction * amount);
    }

    private static int nextPresetColor(int current) {
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            if (PRESET_COLORS[i] == current) {
                return PRESET_COLORS[(i + 1) % PRESET_COLORS.length];
            }
        }
        return PRESET_COLORS[0];
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
            groupColor.setTextColor(parseColor(groupColor.getValue()) == null ? ERROR : 0xFFE0E0E0);
        }
        if (groupName != null) {
            groupName.setTextColor(groupName.getValue().isBlank() ? ERROR : 0xFFE0E0E0);
        }
    }

    private TabNameDimmerConfig.Profile profile() {
        return serverProfile ? config.getOrCreateServerProfile(serverScope) : config.globalProfile;
    }

    private TabNameDimmerConfig.PlayerGroup group() {
        List<TabNameDimmerConfig.PlayerGroup> list = profile().groups;
        return list.get(Math.max(0, Math.min(groupIndex, list.size() - 1)));
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

    private Component deleteProfileLabel() {
        return DELETE_PROFILE.equals(armedDelete)
                ? Component.translatable("tabnamedimmer.button.confirm").withStyle(ChatFormatting.RED)
                : Component.translatable("tabnamedimmer.button.delete_server_profile");
    }

    private Component deleteGroupLabel() {
        return DELETE_GROUP.equals(armedDelete)
                ? Component.translatable("tabnamedimmer.button.confirm").withStyle(ChatFormatting.RED)
                : Component.translatable("tabnamedimmer.button.delete_group");
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

    private final class GroupList extends ObjectSelectionList<GroupEntry> {
        private boolean quiet;

        GroupList(Minecraft minecraft) {
            super(minecraft, 0, 0, 0, 20);
        }

        void fill(List<TabNameDimmerConfig.PlayerGroup> list, int selected) {
            quiet = true;
            clearEntries();
            for (int i = 0; i < list.size(); i++) {
                GroupEntry entry = new GroupEntry(list.get(i), i);
                addEntry(entry);
                if (i == selected) {
                    setSelected(entry);
                }
            }
            refreshScrollAmount();
            quiet = false;
        }

        @Override
        public void setSelected(GroupEntry entry) {
            super.setSelected(entry);
            if (!quiet && entry != null) {
                select(entry.index);
            }
        }

        @Override
        public int getRowLeft() {
            return getX() + 4;
        }

        @Override
        public int getRowWidth() {
            return this.width - (scrollable() ? 18 : 8);
        }

        @Override
        protected int scrollBarX() {
            return getRight() - 10;
        }
    }

    private final class GroupEntry extends ObjectSelectionList.Entry<GroupEntry> {
        private final TabNameDimmerConfig.PlayerGroup group;
        private final int index;

        GroupEntry(TabNameDimmerConfig.PlayerGroup group, int index) {
            this.group = group;
            this.index = index;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int x = getContentX() + 2;
            int middle = getContentYMiddle();
            graphics.fill(x, middle - 4, x + 9, middle + 5, 0xFF000000);
            graphics.fill(x + 1, middle - 3, x + 8, middle + 4, 0xFF000000 | group.color);
            String count = Integer.toString(group.members.size());
            int countX = getContentRight() - 2 - font.width(count);
            graphics.text(font, count, countX, middle - 4, TEXT_DIM, false);
            int room = countX - x - 18;
            String name = font.width(group.name) <= room ? group.name
                    : font.plainSubstrByWidth(group.name, Math.max(0, room - font.width("..."))) + "...";
            Component label = group.enabled ? Component.literal(name) : Component.literal(name).withStyle(ChatFormatting.ITALIC);
            graphics.text(font, label, x + 14, middle - 4, group.enabled ? TEXT : TEXT_DIM, true);
        }

        @Override
        public Component getNarration() {
            return CommonComponents.joinForNarration(Component.literal(group.name),
                    Component.translatable("tabnamedimmer.button.manage_members_count", group.members.size()));
        }
    }

    private static final class SwatchButton extends AbstractButton {
        private final IntSupplier color;
        private final Runnable action;

        SwatchButton(IntSupplier color, Runnable action) {
            super(0, 0, 20, 20, Component.translatable("tabnamedimmer.tooltip.color_preset"));
            this.color = color;
            this.action = action;
            setTooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.color_preset")));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            action.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            extractDefaultSprite(graphics);
            int x = getX() + 5;
            int y = getY() + 5;
            graphics.fill(x - 1, y - 1, x + 11, y + 11, 0xFF000000);
            graphics.fill(x, y, x + 10, y + 10, 0xFF000000 | color.getAsInt());
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
