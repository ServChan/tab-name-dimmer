package org.lts.tabnamedimmer.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class OptionList extends ContainerObjectSelectionList<OptionList.Row> {
    public static final int ROW = 24;
    public static final int SWITCH_WIDTH = 44;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A0A0;

    private final Font font;
    private final int maxRowWidth;
    private final List<Runnable> refreshers = new ArrayList<>();
    private Runnable changed = this::refresh;

    public OptionList(Minecraft minecraft, int maxRowWidth) {
        super(minecraft, 0, 0, 0, ROW);
        this.font = minecraft.font;
        this.maxRowWidth = maxRowWidth;
    }

    public void place(int x, int y, int width, int height) {
        updateSizeAndPosition(width, height, x, y);
        boolean changed = false;
        for (Row row : children()) {
            if (row instanceof TextRow text) {
                int rowHeight = text.wrap(getRowWidth() - 4);
                if (rowHeight != text.getHeight()) {
                    text.setHeight(rowHeight);
                    changed = true;
                }
            }
        }
        if (changed) {
            updateSizeAndPosition(width, height, x, y);
        }
    }

    @Override
    public int getRowWidth() {
        return Math.max(120, Math.min(maxRowWidth, this.width - 32));
    }

    public void onChange(Runnable action) {
        this.changed = action;
    }

    public void refresh() {
        for (Runnable refresher : refreshers) {
            refresher.run();
        }
    }

    public void header(Component text) {
        int padding = children().isEmpty() ? 0 : 10;
        addEntry(new HeaderRow(text, padding), padding + 20);
    }

    public void text(Component text) {
        TextRow row = new TextRow(text);
        addEntry(row, row.wrap(getRowWidth() - 4));
    }

    public CycleButton<Boolean> toggle(Component label, Component hint, BooleanSupplier value, Consumer<Boolean> change, BooleanSupplier active) {
        CycleButton<Boolean> button = CycleButton.onOffBuilder(value.getAsBoolean())
                .displayOnlyValue()
                .withTooltip(state -> hint == null ? null : Tooltip.create(hint))
                .withCustomNarration(cycle -> CommonComponents.joinForNarration(label, cycle.createDefaultNarrationMessage()))
                .create(0, 0, SWITCH_WIDTH, 20, label, (cycle, state) -> {
                    change.accept(state);
                    changed.run();
                });
        refreshers.add(() -> {
            button.setValue(value.getAsBoolean());
            button.active = active == null || active.getAsBoolean();
        });
        addEntry(new LabeledRow(label, hint, List.of(button)), ROW);
        return button;
    }

    public <T> CycleButton<T> cycle(Component label, Component hint, List<T> values, Function<T, Component> names, Supplier<T> value,
            Consumer<T> change, BooleanSupplier active) {
        int widest = 0;
        for (T candidate : values) {
            widest = Math.max(widest, font.width(names.apply(candidate)));
        }
        int width = Math.min(150, Math.max(SWITCH_WIDTH + 16, widest + 20));
        CycleButton<T> button = CycleButton.builder(names, value.get())
                .withValues(values)
                .displayOnlyValue()
                .withTooltip(state -> hint == null ? null : Tooltip.create(hint))
                .withCustomNarration(cycle -> CommonComponents.joinForNarration(label, cycle.createDefaultNarrationMessage()))
                .create(0, 0, width, 20, label, (cycle, state) -> {
                    change.accept(state);
                    changed.run();
                });
        refreshers.add(() -> {
            button.setValue(value.get());
            button.active = active == null || active.getAsBoolean();
        });
        addEntry(new LabeledRow(label, hint, List.of(button)), ROW);
        return button;
    }

    public Button action(Component label, Component hint, Supplier<Component> value, Runnable press, BooleanSupplier active) {
        int width = Math.min(150, Math.max(SWITCH_WIDTH + 16, font.width(value.get()) + 20));
        Button button = Button.builder(value.get(), pressed -> {
            press.run();
            changed.run();
        }).bounds(0, 0, width, 20).createNarration(narration -> CommonComponents.joinForNarration(label, narration.get())).build();
        if (hint != null) {
            button.setTooltip(Tooltip.create(hint));
        }
        refreshers.add(() -> {
            button.setMessage(value.get());
            button.active = active == null || active.getAsBoolean();
        });
        addEntry(new LabeledRow(label, hint, List.of(button)), ROW);
        return button;
    }

    public <W extends AbstractWidget> W labeled(Component label, Component hint, W widget) {
        addEntry(new LabeledRow(label, hint, List.of(widget)), ROW);
        return widget;
    }

    public void group(Component label, Component hint, List<? extends AbstractWidget> widgets) {
        addEntry(new LabeledRow(label, hint, List.copyOf(widgets)), ROW);
    }

    public void buttons(List<? extends AbstractWidget> widgets) {
        addEntry(new ButtonsRow(List.copyOf(widgets)), ROW + 4);
    }

    public abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
        protected List<AbstractWidget> widgets() {
            return List.of();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets();
        }
    }

    private final class HeaderRow extends Row {
        private final Component text;
        private final int padding;

        HeaderRow(Component text, int padding) {
            this.text = text;
            this.padding = padding;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            graphics.centeredText(font, text, getContentXMiddle(), getY() + padding + 6, TEXT);
        }
    }

    private final class TextRow extends Row {
        private final Component text;
        private List<FormattedCharSequence> lines = List.of();

        TextRow(Component text) {
            this.text = text;
        }

        int wrap(int width) {
            lines = font.split(text, Math.max(40, width));
            return lines.size() * 10 + 6;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            for (int i = 0; i < lines.size(); i++) {
                graphics.text(font, lines.get(i), getContentX(), getContentY() + 1 + i * 10, TEXT_DIM, false);
            }
        }
    }

    private final class LabeledRow extends Row {
        private static final int GAP = 2;
        private final Component label;
        private final Component hint;
        private final List<AbstractWidget> widgets;

        LabeledRow(Component label, Component hint, List<AbstractWidget> widgets) {
            this.label = label;
            this.hint = hint;
            this.widgets = widgets;
        }

        @Override
        protected List<AbstractWidget> widgets() {
            return widgets;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int right = getContentRight();
            for (int i = widgets.size() - 1; i >= 0; i--) {
                AbstractWidget widget = widgets.get(i);
                right -= widget.getWidth();
                widget.setPosition(right, getContentY());
                widget.extractRenderState(graphics, mouseX, mouseY, a);
                right -= GAP;
            }
            int room = right - getContentX() - 6;
            String full = label.getString();
            String shown = font.width(full) <= room ? full : font.plainSubstrByWidth(full, Math.max(0, room - font.width("..."))) + "...";
            int x = getContentX();
            int y = getContentY();
            graphics.text(font, shown, x, y + 6, widgets.getFirst().active ? TEXT : TEXT_DIM, true);
            boolean over = mouseX >= x && mouseX < x + font.width(shown) && mouseY >= y + 4 && mouseY < y + 16;
            boolean clipped = !shown.equals(full);
            if (over && (hint != null || clipped)) {
                Component tip = hint == null ? label : clipped ? label.copy().append("\n").append(hint) : hint;
                graphics.setTooltipForNextFrame(font, tip, mouseX, mouseY);
            }
        }
    }

    private final class ButtonsRow extends Row {
        private final List<AbstractWidget> buttons;

        ButtonsRow(List<AbstractWidget> buttons) {
            this.buttons = buttons;
        }

        @Override
        protected List<AbstractWidget> widgets() {
            return buttons;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int gap = 4;
            int each = Math.min(150, (getContentWidth() - gap * (buttons.size() - 1)) / buttons.size());
            int x = getContentXMiddle() - (each * buttons.size() + gap * (buttons.size() - 1)) / 2;
            for (AbstractWidget button : buttons) {
                button.setWidth(each);
                button.setPosition(x, getContentY() + 4);
                button.extractRenderState(graphics, mouseX, mouseY, a);
                x += each + gap;
            }
        }
    }
}
