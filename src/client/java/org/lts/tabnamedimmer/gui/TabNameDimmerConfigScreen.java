package org.lts.tabnamedimmer.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lts.tabnamedimmer.ServerScopeTracker;
import org.lts.tabnamedimmer.TabNameDimmerClient;
import org.lts.tabnamedimmer.TabNameDimmerConfig;

import java.util.Locale;

public class TabNameDimmerConfigScreen extends Screen {
    private static final int FIELD_HEIGHT = 20;
    private static final float[] OPACITY_PRESETS = {0.15F, 0.3F, 0.5F, 0.75F, 1.0F};
    private static final float[] SPEED_PRESETS = {0.01F, 0.025F, 0.05F, 0.1F, 0.2F};
    private static final SystemToast.SystemToastId SAVE_TOAST_ID = new SystemToast.SystemToastId();

    private final Screen parent;
    private final TabNameDimmerConfig config;
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
        int contentWidth = Math.min(520, Math.max(1, width - 20));
        int left = (width - contentWidth) / 2;
        int gap = 8;
        int columnWidth = (contentWidth - gap) / 2;
        int y = 42;

        addPair(left, y, columnWidth, gap,
                Button.builder(enabledLabel(), button -> {
                    config.enabled = !config.enabled;
                    button.setMessage(enabledLabel());
                }).build(),
                Button.builder(modeLabel(), button -> {
                    config.displayMode = next(config.displayMode, TabNameDimmerConfig.DisplayMode.values());
                    button.setMessage(modeLabel());
                }).build());
        y += 24;

        addPair(left, y, columnWidth, gap,
                Button.builder(caseSensitiveLabel(), button -> {
                    config.caseSensitive = !config.caseSensitive;
                    button.setMessage(caseSensitiveLabel());
                }).build(),
                Button.builder(activationLabel(), button -> {
                    config.activationMode = next(config.activationMode, TabNameDimmerConfig.ActivationMode.values());
                    button.setMessage(activationLabel());
                }).build());
        y += 24;

        addPair(left, y, columnWidth, gap,
                Button.builder(notificationsLabel(), button -> {
                    config.notificationsEnabled = !config.notificationsEnabled;
                    button.setMessage(notificationsLabel());
                }).build(),
                Button.builder(transparencyLabel(), button -> {
                    config.playerTransparencyEnabled = !config.playerTransparencyEnabled;
                    button.setMessage(transparencyLabel());
                }).build());
        y += 24;

        addPair(left, y, columnWidth, gap,
                Button.builder(compactNotificationsLabel(), button -> {
                    config.compactNotifications = !config.compactNotifications;
                    button.setMessage(compactNotificationsLabel());
                }).build(),
                Button.builder(notificationSoundsLabel(), button -> {
                    config.notificationSoundsEnabled = !config.notificationSoundsEnabled;
                    button.setMessage(notificationSoundsLabel());
                }).build());
        y += 24;

        addPair(left, y, columnWidth, gap,
                Button.builder(opacityLabel(), button -> {
                    config.dimOpacity = nextPreset(config.dimOpacity, OPACITY_PRESETS);
                    button.setMessage(opacityLabel());
                }).build(),
                Button.builder(speedLabel(), button -> {
                    config.animationSpeed = nextPreset(config.animationSpeed, SPEED_PRESETS);
                    button.setMessage(speedLabel());
                }).build());
        y += 24;

        addPair(left, y, columnWidth, gap,
                Button.builder(sortLabel(), button -> {
                    config.playerSortMode = next(config.playerSortMode, TabNameDimmerConfig.PlayerSortMode.values());
                    button.setMessage(sortLabel());
                }).build(),
                Button.builder(afkHandlingLabel(), button -> {
                    config.afkHandlingMode = next(config.afkHandlingMode, TabNameDimmerConfig.AfkHandlingMode.values());
                    button.setMessage(afkHandlingLabel());
                }).build());
        y += 24;

        addPair(left, y, columnWidth, gap,
                Button.builder(Component.translatable("tabnamedimmer.button.manage_groups"), button ->
                        ScreenNavigator.show(minecraft, new GroupManagerScreen(this, config,
                                ServerScopeTracker.currentScope()))).build(),
                Button.builder(Component.translatable("tabnamedimmer.button.hud_settings"), button ->
                        ScreenNavigator.show(minecraft, new HudSettingsScreen(this, config))).build());

        int bottom = height - 30;
        int actionWidth = (contentWidth - gap) / 2;
        addRenderableWidget(Button.builder(Component.translatable("tabnamedimmer.button.save"), button -> saveAndClose())
                .bounds(left, bottom, actionWidth, FIELD_HEIGHT).build());
        cancelButton = addRenderableWidget(Button.builder(cancelLabel(), button -> closeWithoutSaving())
                .bounds(left + actionWidth + gap, bottom, actionWidth, FIELD_HEIGHT).build());
        refreshDirty();
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

    private void addPair(int left, int y, int width, int gap, Button first, Button second) {
        first.setRectangle(width, FIELD_HEIGHT, left, y);
        second.setRectangle(width, FIELD_HEIGHT, left + width + gap, y);
        addRenderableWidget(first);
        addRenderableWidget(second);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, width, height, 0xF00D141F);

        graphics.fill(0, 0, width, 28, 0xFF182638);
        graphics.fill(0, 27, width, 28, 0xFF2E435E);
        int titleX = (width - font.width(title)) / 2;
        graphics.text(font, title, titleX, 9, 0xFFFFFFFF, true);
        if (dirty) {
            graphics.centeredText(font, Component.translatable("tabnamedimmer.state.unsaved"), width / 2, 31, 0xFFFFC857);
        }

        graphics.centeredText(font, Component.translatable("tabnamedimmer.activation.key_hint"),
                width / 2, height - 44, 0xFF00E5FF);
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
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
        closeWithoutSaving();
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
            TabNameDimmerClient.LOGGER.warn("Unable to show the save notification", exception);
        }
    }

    private Component enabledLabel() {
        return Component.translatable("tabnamedimmer.option.enabled", onOff(config.enabled));
    }

    private Component modeLabel() {
        return Component.translatable("tabnamedimmer.option.mode",
                Component.translatable("tabnamedimmer.mode." + enumKey(config.displayMode)));
    }

    private Component caseSensitiveLabel() {
        return Component.translatable("tabnamedimmer.option.case_sensitive", onOff(config.caseSensitive));
    }

    private Component activationLabel() {
        return Component.translatable("tabnamedimmer.option.activation",
                Component.translatable("tabnamedimmer.activation." + enumKey(config.activationMode)));
    }

    private Component notificationsLabel() {
        return Component.translatable("tabnamedimmer.option.notifications", onOff(config.notificationsEnabled));
    }

    private Component compactNotificationsLabel() {
        return Component.translatable("tabnamedimmer.option.compact_notifications", onOff(config.compactNotifications));
    }

    private Component notificationSoundsLabel() {
        return Component.translatable("tabnamedimmer.option.notification_sounds", onOff(config.notificationSoundsEnabled));
    }

    private Component transparencyLabel() {
        return Component.translatable("tabnamedimmer.option.player_transparency", onOff(config.playerTransparencyEnabled));
    }

    private Component opacityLabel() {
        return Component.translatable("tabnamedimmer.option.player_opacity", Math.round(config.dimOpacity * 100.0F));
    }

    private Component speedLabel() {
        float percent = config.animationSpeed * 100.0F;
        String value = percent == Math.round(percent)
                ? Integer.toString(Math.round(percent)) : String.format(Locale.ROOT, "%.1f", percent);
        return Component.translatable("tabnamedimmer.option.animation_speed", value);
    }

    private Component sortLabel() {
        return Component.translatable("tabnamedimmer.option.player_sort",
                Component.translatable("tabnamedimmer.sort." + enumKey(config.playerSortMode)));
    }

    private Component afkHandlingLabel() {
        return Component.translatable("tabnamedimmer.option.afk_handling",
                Component.translatable("tabnamedimmer.afk." + enumKey(config.afkHandlingMode)));
    }

    static Component onOff(boolean value) {
        return Component.translatable(value ? "tabnamedimmer.state.on" : "tabnamedimmer.state.off")
                .withStyle(style -> style.withColor(value ? 0xFF55FF55 : 0xFFFF5555));
    }

    static String enumKey(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static <T> T next(T current, T[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                return values[(i + 1) % values.length];
            }
        }
        return values[0];
    }

    private static float nextPreset(float current, float[] presets) {
        for (float preset : presets) {
            if (preset > current + 0.0001F) {
                return preset;
            }
        }
        return presets[0];
    }
}
