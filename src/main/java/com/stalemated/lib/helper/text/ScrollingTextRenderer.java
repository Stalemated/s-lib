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

/**
 * Draws one line of text. If the text is wider than its space, it scrolls back and forth at
 * 25 pixels per second and stops for 1.5 seconds at each end.
 * <p>
 * It does not clip: enable a scissor around the area before drawing. Use one instance per
 * text on screen. The instance keeps the scroll time and starts over when the text changes
 * or when it was not drawn for more than 100 ms.
 * <p>
 * The draw method is {@code render} before 26.1 and {@code extract} from 26.1. Client only.
 */
public class ScrollingTextRenderer {

    private static final double SCROLL_SPEED_PIXELS_PER_SECOND = 25.0;
    private static final long SCROLL_PAUSE_MS = 1500L;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DISABLED_TEXT_COLOR = 0xFFAAAAAA;

    private final ScrollClock clock = new ScrollClock();

    //? if <26.1 {
    /**
     * Draws {@code text} at {@code (x, y)}, scrolling horizontally when wider than
     * {@code availableWidth}.
     *
     * @param context the GUI graphics context
     * @param text the text to draw
     * @param x the left edge of the visible area
     * @param y the top edge of the text
     * @param availableWidth the width of the visible area in pixels
     * @param isDisabled {@code true} to draw in gray ({@code 0xFFAAAAAA}) instead of white
     */
    public void render(GuiGraphics context, String text, int x, int y, int availableWidth, boolean isDisabled) {
    //?} else {
    /*/^*
     * Draws {@code text} at {@code (x, y)}, scrolling horizontally when wider than
     * {@code availableWidth}.
     *
     * @param context the GUI graphics extractor
     * @param text the text to draw
     * @param x the left edge of the visible area
     * @param y the top edge of the text
     * @param availableWidth the width of the visible area in pixels
     * @param isDisabled {@code true} to draw in gray ({@code 0xFFAAAAAA}) instead of white
     ^/
    public void extract(GuiGraphicsExtractor context, String text, int x, int y, int availableWidth, boolean isDisabled) {
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
