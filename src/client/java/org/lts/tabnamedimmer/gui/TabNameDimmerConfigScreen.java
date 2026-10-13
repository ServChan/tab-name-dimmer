package org.lts.tabnamedimmer.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lts.tabnamedimmer.ServerScopeTracker;
import org.lts.tabnamedimmer.TabNameDimmerClient;
import org.lts.tabnamedimmer.TabNameDimmerConfig;

import java.util.List;
import java.util.Locale;

public class TabNameDimmerConfigScreen extends FramedScreen {
    private static final int ROW_WIDTH = 310;
    private static final List<Float> OPACITY_PRESETS = List.of(0.15F, 0.3F, 0.5F, 0.75F, 1.0F);
    private static final List<Float> SPEED_PRESETS = List.of(0.01F, 0.025F, 0.05F, 0.1F, 0.2F);
    private static final SystemToast.SystemToastId SAVE_TOAST_ID = new SystemToast.SystemToastId();

    private final Screen parent;
    private final TabNameDimmerConfig config;
    private OptionList list;
    private Button cancelButton;
    private boolean dirty;
    private int dirtyCheckTicks;

    public TabNameDimmerConfigScreen(Screen parent) {
        this(parent, TabNameDimmerConfig.currentCopy());
    }

    TabNameDimmerConfigScreen(Screen parent, TabNameDimmerConfig config) {
        super(Component.translatable("tabnamedimmer.screen.title"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        double scroll = list == null ? 0 : list.scrollAmount();
        list = new OptionList(minecraft, ROW_WIDTH);
        list.place(0, HEADER, width, height - HEADER - FOOTER);

        list.header(Component.translatable("tabnamedimmer.section.general"));
        list.toggle(Component.translatable("tabnamedimmer.option.enabled"), null,
                () -> config.enabled, value -> config.enabled = value, null);
        list.cycle(Component.translatable("tabnamedimmer.option.mode"), null, List.of(TabNameDimmerConfig.DisplayMode.values()),
                mode -> Component.translatable("tabnamedimmer.mode." + enumKey(mode)),
                () -> config.displayMode, value -> config.displayMode = value, this::enabled);
        list.cycle(Component.translatable("tabnamedimmer.option.activation"), Component.translatable("tabnamedimmer.activation.key_hint"),
                List.of(TabNameDimmerConfig.ActivationMode.values()),
                mode -> Component.translatable("tabnamedimmer.activation." + enumKey(mode)),
                () -> config.activationMode, value -> config.activationMode = value, this::enabled);
        list.toggle(Component.translatable("tabnamedimmer.option.case_sensitive"), null,
                () -> config.caseSensitive, value -> config.caseSensitive = value, this::enabled);

        list.header(Component.translatable("tabnamedimmer.section.players"));
        list.toggle(Component.translatable("tabnamedimmer.option.player_transparency"), null,
                () -> config.playerTransparencyEnabled, value -> config.playerTransparencyEnabled = value, this::enabled);
        list.cycle(Component.translatable("tabnamedimmer.option.player_opacity"), null, OPACITY_PRESETS,
                value -> Component.literal(Math.round(value * 100.0F) + "%"),
                () -> config.dimOpacity, value -> config.dimOpacity = value,
                () -> config.enabled && config.playerTransparencyEnabled);
        list.cycle(Component.translatable("tabnamedimmer.option.animation_speed"), null, SPEED_PRESETS,
                TabNameDimmerConfigScreen::speedLabel,
                () -> config.animationSpeed, value -> config.animationSpeed = value, this::enabled);
        list.cycle(Component.translatable("tabnamedimmer.option.player_sort"), null, List.of(TabNameDimmerConfig.PlayerSortMode.values()),
                mode -> Component.translatable("tabnamedimmer.sort." + enumKey(mode)),
                () -> config.playerSortMode, value -> config.playerSortMode = value, this::enabled);
        list.cycle(Component.translatable("tabnamedimmer.option.afk_handling"), null, List.of(TabNameDimmerConfig.AfkHandlingMode.values()),
                mode -> Component.translatable("tabnamedimmer.afk." + enumKey(mode)),
                () -> config.afkHandlingMode, value -> config.afkHandlingMode = value, this::enabled);
        EditBox afkIcons = new EditBox(font, 0, 0, 120, 20, Component.translatable("tabnamedimmer.field.afk_icons"));
        afkIcons.setMaxLength(TabNameDimmerConfig.MAX_AFK_ICONS_LENGTH);
        afkIcons.setValue(config.afkIcons);
        afkIcons.setHint(Component.translatable("tabnamedimmer.hint.afk_icons"));
        afkIcons.setTooltip(Tooltip.create(Component.translatable("tabnamedimmer.tooltip.afk_icons")));
        afkIcons.setResponder(value -> config.afkIcons = value.trim());
        list.labeled(Component.translatable("tabnamedimmer.option.afk_icons"), Component.translatable("tabnamedimmer.tooltip.afk_icons"), afkIcons);

        list.header(Component.translatable("tabnamedimmer.section.notifications"));
        list.toggle(Component.translatable("tabnamedimmer.option.notifications"), null,
                () -> config.notificationsEnabled, value -> config.notificationsEnabled = value, null);
        list.toggle(Component.translatable("tabnamedimmer.option.compact_notifications"), null,
                () -> config.compactNotifications, value -> config.compactNotifications = value, () -> config.notificationsEnabled);
        list.toggle(Component.translatable("tabnamedimmer.option.notification_sounds"), null,
                () -> config.notificationSoundsEnabled, value -> config.notificationSoundsEnabled = value, () -> config.notificationsEnabled);

        list.buttons(List.of(
                Button.builder(Component.translatable("tabnamedimmer.button.manage_groups"), button ->
                        ScreenNavigator.show(minecraft, new GroupManagerScreen(this, config, ServerScopeTracker.currentScope()))).build(),
                Button.builder(Component.translatable("tabnamedimmer.button.hud_settings"), button ->
                        ScreenNavigator.show(minecraft, new HudSettingsScreen(this, config))).build()));
        list.refresh();
        list.setScrollAmount(scroll);
        addRenderableWidget(list);

        int y = footerButtonsY();
        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.save"), button -> saveAndClose())
                .bounds(width / 2 - 155, y, 150, 20).build());
        cancelButton = addRenderableWidget(Button.builder(cancelLabel(), button -> closeWithoutSaving())
                .bounds(width / 2 + 5, y, 150, 20).build());
        refreshDirty();
    }

    private boolean enabled() {
        return config.enabled;
    }

    @Override
    public void tick() {
        super.tick();
        if (++dirtyCheckTicks >= 5) {
            refreshDirty();
        }
    }

    private void refreshDirty() {
        dirtyCheckTicks = 0;
        dirty = !config.sameSettingsAs(TabNameDimmerConfig.current());
        if (cancelButton != null) {
            cancelButton.setMessage(cancelLabel());
        }
    }

    private Component cancelLabel() {
        return Component.translatable(dirty ? "tabnamedimmer.button.discard" : "tabnamedimmer.button.cancel");
    }

    @Override
    protected Component subtitle() {
        return dirty ? Component.translatable("tabnamedimmer.state.unsaved") : null;
    }

    @Override
    protected int subtitleColor() {
        return NOTICE;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_S && event.hasControlDown()) {
            saveAndClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (config.sameSettingsAs(TabNameDimmerConfig.current())) {
            closeWithoutSaving();
        } else {
            saveAndClose();
        }
    }

    private void saveAndClose() {
        if (!TabNameDimmerConfig.save(config)) {
            showToast("tabnamedimmer.toast.save_failed");
            return;
        }
        showToast("tabnamedimmer.toast.saved.description");
        ScreenNavigator.show(minecraft, parent);
    }

    private void closeWithoutSaving() {
        ScreenNavigator.show(minecraft, parent);
    }

    private void showToast(String descriptionKey) {
        try {
            ToastManager manager;
            try {
                manager = (ToastManager) minecraft.gui.getClass().getMethod("toastManager").invoke(minecraft.gui);
            } catch (NoSuchMethodException ignored) {
                manager = (ToastManager) minecraft.getClass().getMethod("getToastManager").invoke(minecraft);
            }
            SystemToast.addOrUpdate(manager, SAVE_TOAST_ID,
                    Component.translatable("tabnamedimmer.toast.saved.title"), Component.translatable(descriptionKey));
        } catch (ReflectiveOperationException exception) {
            TabNameDimmerClient.LOGGER.warn("Could not show the save notification", exception);
        }
    }

    private static Component speedLabel(float speed) {
        float percent = speed * 100.0F;
        String value = percent == Math.round(percent)
                ? Integer.toString(Math.round(percent)) : String.format(Locale.ROOT, "%.1f", percent);
        return Component.literal(value + "%");
    }

    static String enumKey(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
