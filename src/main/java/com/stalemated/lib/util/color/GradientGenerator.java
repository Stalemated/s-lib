package com.stalemated.lib.util.color;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class GradientGenerator {

    public static MutableComponent getStaticGradient(Component text, int color1, int color2) {
        return applyGradient(text, (index, ratio) ->
                GradientColorUtils.interpolate(color1, color2, ratio)
        );
    }

    public static MutableComponent getSlideGradient(Component text, int offset, int color1, int color2, int tickrate, boolean reversed) {
        long time = getAnimationTime(tickrate);
        int dir = reversed ? -1 : 1;
        return applyGradient(text, (index, ratio) -> {
            float hue = (float) (((time - (index * dir) - offset) % 45.0) / 22.5f);
            return GradientColorUtils.gradientSlide(hue, color1, color2);
        });
    }

    public static MutableComponent getBreathingGradient(Component text, int offset, int color1, int color2, int tickrate, boolean reversed) {
        long time = getAnimationTime(tickrate);
        int dir = reversed ? -1 : 1;
        return applyGradient(text, (index, ratio) -> {
            float animationFactor = (float) ((Math.sin((time - (index * dir) - offset) * 0.05) + 1.0) / 2.0);
            return GradientColorUtils.interpolateAnimation(color1, color2, ratio, animationFactor);
        });
    }

    public static MutableComponent getRainbowGradient(Component text, int offset, int tickrate, boolean reversed) {
        long time = getAnimationTime(tickrate);
        int dir = reversed ? -1 : 1;
        return applyGradient(text, (index, ratio) -> {
            float hue = (float) (((time - (index * dir) - offset) / 90.0) % 1.0);
            return Color.HSBtoRGB(hue, 0.5F, 1.0F);
        });
    }

    // Helpers

    private static long getAnimationTime(int tickrate) {
        return (long) (System.currentTimeMillis() / (double) Math.max(1, tickrate) * 3.0);
    }

    @FunctionalInterface
    private interface GradientColorProvider {
        int getColor(int index, float ratio);
    }

    private record StyledCodePoint(Style style, int codePoint) {}

    private static MutableComponent applyGradient(Component text, GradientColorProvider colorProvider) {
        List<StyledCodePoint> codePoints = new ArrayList<>();
        text.visit((style, segment) -> {
            segment.codePoints().forEach(codePoint -> codePoints.add(new StyledCodePoint(style, codePoint)));
            return Optional.empty();
        }, Style.EMPTY);

        MutableComponent gradientText = Component.empty();
        float maxIndex = Math.max(1.0f, codePoints.size() - 1.0f);

        for (int i = 0; i < codePoints.size(); i++) {
            StyledCodePoint glyph = codePoints.get(i);
            int color = colorProvider.getColor(i, i / maxIndex);

            gradientText.append(Component.literal(Character.toString(glyph.codePoint()))
                    .setStyle(glyph.style().withColor(color)));
        }

        return gradientText;
    }
}
