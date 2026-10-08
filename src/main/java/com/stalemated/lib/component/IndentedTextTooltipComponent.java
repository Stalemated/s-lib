package com.stalemated.lib.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.util.FormattedCharSequence;
//? if <1.21.11 {
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
//?} elif <26.1 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?}

public class IndentedTextTooltipComponent extends ClientTextTooltip {

    private final int xOffset;
    public IndentedTextTooltipComponent(FormattedCharSequence text, int xOffset) {
        super(text);
        this.xOffset = xOffset;
    }

    @Override
    public int getWidth(Font textRenderer) {
        return super.getWidth(textRenderer) + this.xOffset;
    }

    //? if <1.21.11 {
    @Override
    public void renderText(Font textRenderer, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource vertexConsumers) {
        super.renderText(textRenderer, x + this.xOffset, y, matrix, vertexConsumers);
    }
    //?} elif <26.1 {
    /*@Override
    public void renderText(GuiGraphics guiGraphics, Font textRenderer, int x, int y) {
        super.renderText(guiGraphics, textRenderer, x + this.xOffset, y);
    }
    *///?} else {
    /*@Override
    public void extractText(GuiGraphicsExtractor guiGraphicsExtractor, Font textRenderer, int x, int y) {
        super.extractText(guiGraphicsExtractor, textRenderer, x + this.xOffset, y);
    }
    *///?}
}
