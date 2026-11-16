package com.stalemated.lib.compat.yacl.builder;

//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?}

/**
 * Draws on top of a screen from {@link YaclScreenBuilder#generateScreen()}, every frame, after
 * YACL has drawn its widgets. Client only.
 * <p>
 * The type of {@code graphics} follows the Minecraft version: {@code GuiGraphics} before 26.1,
 * {@code GuiGraphicsExtractor} from 26.1 on.
 *
 * @see YaclScreenBuilder#onRender(ScreenOverlay)
 */
@FunctionalInterface
public interface ScreenOverlay {
    /**
     * Draws the overlay for this frame.
     *
     * @param graphics the graphics of the screen
     * @param mouseX mouse x in GUI pixels
     * @param mouseY mouse y in GUI pixels
     * @param partialTick fraction of the current tick, from 0 to 1
     */
    //? if <26.1 {
    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
    //?} else {
    /*void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick);
    *///?}
}
