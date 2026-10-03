package com.stalemated.lib.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

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

    @Override
    public void renderText(Font textRenderer, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource vertexConsumers) {
        super.renderText(textRenderer, x + this.xOffset, y, matrix, vertexConsumers);
    }
}
