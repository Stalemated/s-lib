package com.stalemated.lib.util.color;

import java.util.List;

public final class VanillaTooltipColors {

    public static final int TEXT = 0xFFFFFF;
    public static final int BORDER_TOP = 0x505000FF;
    public static final int BORDER_BOTTOM = 0x5028007F;
    public static final int BACKGROUND = 0xF0100010;
    public static final int BACKGROUND_OPACITY = 0xF0;
    public static final List<Integer> BORDER = List.of(BORDER_TOP, BORDER_BOTTOM);
    public static final List<Integer> BACKGROUND_GRADIENT = List.of(BACKGROUND, BACKGROUND);

    private VanillaTooltipColors() {}
}
