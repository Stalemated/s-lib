package com.stalemated.lib.util.color;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.awt.*;

public class GradientGenerator {

    public static MutableText getStaticGradient(Text text, int color1, int color2) {
        return applyGradient(text.getString(), (index, ratio) -> 
                GradientColorUtils.interpolate(color1, color2, ratio)
        );
    }

    public static MutableText getSlideGradient(Text text, int offset, int color1, int color2, int tickrate, boolean reversed) {
        long time = getAnimationTime(tickrate);
        int dir = reversed ? -1 : 1;
        return applyGradient(text.getString(), (index, ratio) -> {
            float hue = (float) (((time - (index * dir) - offset) % 45.0) / 22.5f);
            return GradientColorUtils.gradientSlide(hue, color1, color2);
        });
    }

    public static MutableText getBreathingGradient(Text text, int offset, int color1, int color2, int tickrate, boolean reversed) {
        long time = getAnimationTime(tickrate);
        int dir = reversed ? -1 : 1;
        return applyGradient(text.getString(), (index, ratio) -> {
            float animationFactor = (float) ((Math.sin((time - (index * dir) - offset) * 0.05) + 1.0) / 2.0);
            return GradientColorUtils.interpolateAnimation(color1, color2, ratio, animationFactor);
        });
    }

    public static MutableText getRainbowGradient(Text text, int offset, int tickrate, boolean reversed) {
        long time = getAnimationTime(tickrate);
        int dir = reversed ? -1 : 1;
        return applyGradient(text.getString(), (index, ratio) -> {
            float hue = (float) ((1.0 / 90.0 * (time - (index * dir) - offset)) % 360);
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

    private static MutableText applyGradient(String text, GradientColorProvider colorProvider) {
        MutableText gradientText = Text.empty();

        float maxIndex = Math.max(1.0f, text.length() - 1.0f);

        for (int i = 0; i < text.length(); i++) {
            float ratio = i / maxIndex;
            int color = colorProvider.getColor(i, ratio);
            
            gradientText.append(Text.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(color)));
        }
        
        return gradientText;
    }
}