package com.stalemated.lib.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.util.FormattedCharSequence;
//? if <1.21.6 {
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
//?} elif <26.1 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?}

/**
 * A {@link ClientTextTooltip} line drawn {@code xOffset} pixels to the right.
 * {@link #getWidth(Font)} includes the indentation. Client only.
 */
public class IndentedTextTooltipComponent extends ClientTextTooltip {

    private final int xOffset;

    /**
     * Creates an indented text tooltip component.
     *
     * @param text the ordered text sequence to render
     * @param xOffset horizontal indentation in pixels; not checked, so a negative value moves
     *                the text left and makes the line narrower
     */
    public IndentedTextTooltipComponent(FormattedCharSequence text, int xOffset) {
        super(text);
        this.xOffset = xOffset;
    }

    /**
     * Returns the text width plus the configured horizontal indentation.
     *
     * @param textRenderer the font used to measure the text
     * @return total component width in pixels
     */
    @Override
    public int getWidth(Font textRenderer) {
        return super.getWidth(textRenderer) + this.xOffset;
    }

    //? if <1.21.6 {
    /**
     * Draws the tooltip text shifted right by {@code xOffset}. Minecraft calls this when it
     * draws the tooltip.
     *
     * @param textRenderer the font to draw with
     * @param x the left edge of the line, without the indentation
     * @param y the top edge of the line
     * @param matrix the pose matrix
     * @param vertexConsumers the buffers to draw into
     */
    @Override
    public void renderText(Font textRenderer, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource vertexConsumers) {
        super.renderText(textRenderer, x + this.xOffset, y, matrix, vertexConsumers);
    }
    //?} elif <26.1 {
    /*/^*
     * Draws the tooltip text shifted right by {@code xOffset}. Minecraft calls this when it
     * draws the tooltip.
     *
     * @param guiGraphics the graphics to draw with
     * @param textRenderer the font to draw with
     * @param x the left edge of the line, without the indentation
     * @param y the top edge of the line
     ^/
    @Override
    public void renderText(GuiGraphics guiGraphics, Font textRenderer, int x, int y) {
        super.renderText(guiGraphics, textRenderer, x + this.xOffset, y);
    }
    *///?} else {
    /*/^*
     * Draws the tooltip text shifted right by {@code xOffset}. Minecraft calls this when it
     * draws the tooltip.
     *
     * @param guiGraphicsExtractor the graphics to draw with
     * @param textRenderer the font to draw with
     * @param x the left edge of the line, without the indentation
     * @param y the top edge of the line
     ^/
    @Override
    public void extractText(GuiGraphicsExtractor guiGraphicsExtractor, Font textRenderer, int x, int y) {
        super.extractText(guiGraphicsExtractor, textRenderer, x + this.xOffset, y);
    }
    *///?}
}
