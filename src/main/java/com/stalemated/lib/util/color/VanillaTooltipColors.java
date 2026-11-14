package com.stalemated.lib.util.color;

import java.util.List;

/**
 * Colors of the old vanilla tooltip, from the versions that drew it with plain colors. Newer
 * versions draw it with sprites. Border and background colors are ARGB ints.
 */
public final class VanillaTooltipColors {

    /**
     * Default RGB tooltip text color ({@code 0xFFFFFF}). It has no alpha byte: from 1.21.6
     * text drawn with alpha 0 is invisible, so use {@code 0xFF000000 | TEXT} there.
     */
    public static final int TEXT = 0xFFFFFF;
    /** Top gradient color of the vanilla tooltip border ({@code 0x505000FF}). */
    public static final int BORDER_TOP = 0x505000FF;
    /** Bottom gradient color of the vanilla tooltip border ({@code 0x5028007F}). */
    public static final int BORDER_BOTTOM = 0x5028007F;
    /** Vanilla tooltip background color ({@code 0xF0100010}). */
    public static final int BACKGROUND = 0xF0100010;
    /** Default alpha byte of the vanilla tooltip background ({@code 0xF0}). */
    public static final int BACKGROUND_OPACITY = 0xF0;
    /** Two-stop vertical border color list {@code [BORDER_TOP, BORDER_BOTTOM]}. */
    public static final List<Integer> BORDER = List.of(BORDER_TOP, BORDER_BOTTOM);
    /** Two-stop uniform background color list {@code [BACKGROUND, BACKGROUND]}. */
    public static final List<Integer> BACKGROUND_GRADIENT = List.of(BACKGROUND, BACKGROUND);

    private VanillaTooltipColors() {}
}
