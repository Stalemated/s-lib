package com.stalemated.lib.helper.text;

import com.stalemated.lib.util.math.ScrollMathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class ScrollingTextRenderer {

    private static final double SCROLL_SPEED_PIXELS_PER_SECOND = 25.0;
    private static final long SCROLL_PAUSE_MS = 1500L;
    private final long startTime;

    public ScrollingTextRenderer() {
        this.startTime = System.currentTimeMillis();
    }

    public void render(DrawContext context, String text, int x, int y, int availableWidth, boolean isDisabled) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textWidth = textRenderer.getWidth(text);

        if (textWidth > availableWidth) {
            int overflowWidth = textWidth - availableWidth;
            long elapsedTime = System.currentTimeMillis() - this.startTime;
            int scrollOffset = ScrollMathUtil.calculateScrollOffset(overflowWidth, SCROLL_SPEED_PIXELS_PER_SECOND, SCROLL_PAUSE_MS, elapsedTime);

            context.drawTextWithShadow(textRenderer, text, x - scrollOffset, y, isDisabled ? 0xAAAAAA : 0xFFFFFF);
        } else {
            context.drawTextWithShadow(textRenderer, text, x, y, isDisabled ? 0xAAAAAA : 0xFFFFFF);
        }
    }


}