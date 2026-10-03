package com.stalemated.lib.helper.text;

import com.stalemated.lib.util.math.ScrollMathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class ScrollingTextRenderer {

    private static final double SCROLL_SPEED_PIXELS_PER_SECOND = 25.0;
    private static final long SCROLL_PAUSE_MS = 1500L;
    private final long startTime;

    public ScrollingTextRenderer() {
        this.startTime = System.currentTimeMillis();
    }

    public void render(GuiGraphics context, String text, int x, int y, int availableWidth, boolean isDisabled) {
        Font textRenderer = Minecraft.getInstance().font;
        int textWidth = textRenderer.width(text);

        if (textWidth > availableWidth) {
            int overflowWidth = textWidth - availableWidth;
            long elapsedTime = System.currentTimeMillis() - this.startTime;
            int scrollOffset = ScrollMathUtil.calculateScrollOffset(overflowWidth, SCROLL_SPEED_PIXELS_PER_SECOND, SCROLL_PAUSE_MS, elapsedTime);

            context.drawString(textRenderer, text, x - scrollOffset, y, isDisabled ? 0xAAAAAA : 0xFFFFFF);
        } else {
            context.drawString(textRenderer, text, x, y, isDisabled ? 0xAAAAAA : 0xFFFFFF);
        }
    }


}
