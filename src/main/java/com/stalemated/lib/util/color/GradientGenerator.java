package com.stalemated.lib.util.color;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Colors each character of a {@link Component} with a gradient and keeps its other styles.
 * Colors are RGB ints: alpha has no effect on text. The animated gradients read the system
 * clock when called, so call them again every frame to animate.
 */
public final class GradientGenerator {

    private GradientGenerator() {}

    /**
     * Applies a static linear gradient from {@code color1} to {@code color2} across {@code text}.
     *
     * @param text the text to color
     * @param color1 the color of the first character
     * @param color2 the color of the last character
     * @return a new styled {@link MutableComponent}
     */
    public static MutableComponent getStaticGradient(Component text, int color1, int color2) {
        return applyGradient(text, (index, ratio) ->
                GradientColorUtils.interpolate(color1, color2, ratio)
        );
    }

    /**
     * Applies a sliding wave gradient between {@code color1} and {@code color2} animated over
     * wall-clock time.
     *
     * @param text the text to color
     * @param offset shifts the animation; give each line a different value so they do not move
     *               together
     * @param color1 the first color
     * @param color2 the second color
     * @param slowdown divides {@link System#currentTimeMillis()}; larger values animate slower,
     *                 and values below 1 count as 1
     * @param reversed {@code true} to reverse the wave travel direction
     * @return a new styled {@link MutableComponent}
     */
    public static MutableComponent getSlideGradient(Component text, int offset, int color1, int color2, int slowdown, boolean reversed) {
        long time = getAnimationTime(slowdown);
        int dir = reversed ? -1 : 1;
        return applyGradient(text, (index, ratio) -> {
            float hue = (float) (((time - (index * dir) - offset) % 45.0) / 22.5f);
            return GradientColorUtils.gradientSlide(hue, color1, color2);
        });
    }

    /**
     * Applies a fixed gradient from {@code color1} to {@code color2} and fades each character
     * toward white and back on a sine wave.
     *
     * @param text the text to color
     * @param offset shifts the animation; give each line a different value so they do not move
     *               together
     * @param color1 the color of the first character
     * @param color2 the color of the last character
     * @param slowdown divides {@link System#currentTimeMillis()}; larger values animate slower,
     *                 and values below 1 count as 1
     * @param reversed {@code true} to reverse the wave travel direction
     * @return a new styled {@link MutableComponent}
     */
    public static MutableComponent getBreathingGradient(Component text, int offset, int color1, int color2, int slowdown, boolean reversed) {
        long time = getAnimationTime(slowdown);
        int dir = reversed ? -1 : 1;
        return applyGradient(text, (index, ratio) -> {
            float animationFactor = (float) ((Math.sin((time - (index * dir) - offset) * 0.05) + 1.0) / 2.0);
            return GradientColorUtils.interpolateAnimation(color1, color2, ratio, animationFactor);
        });
    }

    /**
     * Applies an animated pastel rainbow gradient across {@code text} (HSB with saturation 0.5
     * and brightness 1).
     *
     * @param text the text to color
     * @param offset shifts the animation; give each line a different value so they do not move
     *               together
     * @param slowdown divides {@link System#currentTimeMillis()}; larger values animate slower,
     *                 and values below 1 count as 1
     * @param reversed {@code true} to reverse the rainbow travel direction
     * @return a new styled {@link MutableComponent}
     */
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
