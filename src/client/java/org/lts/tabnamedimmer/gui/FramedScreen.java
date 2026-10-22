package org.lts.tabnamedimmer.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

abstract class FramedScreen extends Screen {
    static final int HEADER = 33;
    static final int FOOTER = 33;
    static final int TEXT = 0xFFFFFFFF;
    static final int TEXT_DIM = 0xFFA0A0A0;
    static final int NOTICE = 0xFFFFFF55;
    static final int ERROR = 0xFFFF5555;

    protected FramedScreen(Component title) {
        super(title);
    }

    protected Component subtitle() {
        return null;
    }

    protected int subtitleColor() {
        return TEXT_DIM;
    }

    protected int footerButtonsY() {
        return this.height - 27;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        boolean inWorld = this.minecraft != null && this.minecraft.level != null;
        Identifier top = inWorld ? Screen.INWORLD_HEADER_SEPARATOR : Screen.HEADER_SEPARATOR;
        Identifier bottom = inWorld ? Screen.INWORLD_FOOTER_SEPARATOR : Screen.FOOTER_SEPARATOR;
        graphics.blit(RenderPipelines.GUI_TEXTURED, top, 0, HEADER - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
        graphics.blit(RenderPipelines.GUI_TEXTURED, bottom, 0, this.height - FOOTER, 0.0F, 0.0F, this.width, 2, 32, 2);
        Component subtitle = subtitle();
        Component title = Component.literal(fit(this.title.getString()));
        if (subtitle == null) {
            graphics.centeredText(this.font, title, this.width / 2, (HEADER - 9) / 2, TEXT);
            return;
        }
        graphics.centeredText(this.font, title, this.width / 2, 6, TEXT);
        graphics.centeredText(this.font, Component.literal(fit(subtitle.getString())), this.width / 2, 18, subtitleColor());
    }

    private String fit(String line) {
        int room = this.width - 16;
        if (this.font.width(line) > room) {
            return this.font.plainSubstrByWidth(line, Math.max(0, room - this.font.width("..."))) + "...";
        }
        return line;
    }
}
