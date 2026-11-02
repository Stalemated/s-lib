package com.stalemated.lib.helper.text;

import com.stalemated.lib.util.math.ScrollClock;
import com.stalemated.lib.util.math.ScrollMathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?}

public class ScrollingTextRenderer {

    private static final double SCROLL_SPEED_PIXELS_PER_SECOND = 25.0;
    private static final long SCROLL_PAUSE_MS = 1500L;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DISABLED_TEXT_COLOR = 0xFFAAAAAA;

    private final ScrollClock clock = new ScrollClock();

    //? if <26.1 {
    public void render(GuiGraphics context, String text, int x, int y, int availableWidth, boolean isDisabled) {
    //?} else {
    /*public void extract(GuiGraphicsExtractor context, String text, int x, int y, int availableWidth, boolean isDisabled) {
    *///?}
        Font textRenderer = Minecraft.getInstance().font;
        int overflowWidth = textRenderer.width(text) - availableWidth;
        int scrollOffset = overflowWidth > 0
                ? ScrollMathUtil.calculateScrollOffset(overflowWidth, SCROLL_SPEED_PIXELS_PER_SECOND, SCROLL_PAUSE_MS, clock.elapsedMillis(text.hashCode()))
                : 0;
        int color = isDisabled ? DISABLED_TEXT_COLOR : TEXT_COLOR;
        //? if <26.1 {
        context.drawString(textRenderer, text, x - scrollOffset, y, color);
        //?} else {
        /*context.text(textRenderer, text, x - scrollOffset, y, color);
        *///?}
    }
}
