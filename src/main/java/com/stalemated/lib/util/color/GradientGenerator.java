package com.stalemated.lib.util.color;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class GradientGenerator {

    private GradientGenerator() {}

    public static MutableComponent getStaticGradient(Component text, int color1, int color2) {
        return applyGradient(text, (index, ratio) ->
                GradientColorUtils.interpolate(color1, color2, ratio)
        );
    }

    public static MutableComponent getSlideGradient(Component text, int offset, int color1, int color2, int slowdown, boolean reversed) {
        long time = getAnimationTime(slowdown);
        int dir = reversed ? -1 : 1;
        return applyGradient(text, (index, ratio) -> {
            float hue = (float) (((time - (index * dir) - offset) % 45.0) / 22.5f);
            return GradientColorUtils.gradientSlide(hue, color1, color2);
        });
    }

    public static MutableComponent getBreathingGradient(Component text, int offset, int color1, int color2, int slowdown, boolean reversed) {
        long time = getAnimationTime(slowdown);
        int dir = reversed ? -1 : 1;
        return applyGradient(text, (index, ratio) -> {
            float animationFactor = (float) ((Math.sin((time - (index * dir) - offset) * 0.05) + 1.0) / 2.0);
            return GradientColorUtils.interpolateAnimation(color1, color2, ratio, animationFactor);
        });
    }

    public static MutableComponent getRainbowGradient(Component text, int offset, int slowdown, boolean reversed) {
        long time = getAnimationTime(slowdown);
        int dir = reversed ? -1 : 1;
        return applyGradient(text, (index, ratio) -> {
            float hue = (float) (((time - (index * dir) - offset) / 90.0) % 1.0);
            return Color.HSBtoRGB(hue, 0.5F, 1.0F);
        });
    }

    // Helpers

    private static long getAnimationTime(int slowdown) {
        return (long) (System.currentTimeMillis() / (double) Math.max(1, slowdown) * 3.0);
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
        StringBuilder run = new StringBuilder();
        Style runStyle = Style.EMPTY;
        int runColor = 0;

        for (int i = 0; i < codePoints.size(); i++) {
            StyledCodePoint glyph = codePoints.get(i);
            int color = colorProvider.getColor(i, i / maxIndex);
            if (!run.isEmpty() && (glyph.style() != runStyle || color != runColor)) {
                appendRun(gradientText, run, runStyle, runColor);
            }
            runStyle = glyph.style();
            runColor = color;
            run.appendCodePoint(glyph.codePoint());
        }
        if (!run.isEmpty()) {
            appendRun(gradientText, run, runStyle, runColor);
        }

        return gradientText;
    }

    private static void appendRun(MutableComponent target, StringBuilder run, Style style, int color) {
        target.append(Component.literal(run.toString()).setStyle(style.withColor(color)));
        run.setLength(0);
    }
}
