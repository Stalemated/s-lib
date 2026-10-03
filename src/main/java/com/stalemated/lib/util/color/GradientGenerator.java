package com.stalemated.lib.util.color;

import java.awt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class GradientGenerator {

    public static MutableComponent getStaticGradient(Component text, int color1, int color2) {
        return applyGradient(text.getString(), (index, ratio) -> 
                GradientColorUtils.interpolate(color1, color2, ratio)
        );
    }

    public static MutableComponent getSlideGradient(Component text, int offset, int color1, int color2, int tickrate, boolean reversed) {
        long time = getAnimationTime(tickrate);
        int dir = reversed ? -1 : 1;
        return applyGradient(text.getString(), (index, ratio) -> {
            float hue = (float) (((time - (index * dir) - offset) % 45.0) / 22.5f);
            return GradientColorUtils.gradientSlide(hue, color1, color2);
        });
    }

    public static MutableComponent getBreathingGradient(Component text, int offset, int color1, int color2, int tickrate, boolean reversed) {
        long time = getAnimationTime(tickrate);
        int dir = reversed ? -1 : 1;
        return applyGradient(text.getString(), (index, ratio) -> {
            float animationFactor = (float) ((Math.sin((time - (index * dir) - offset) * 0.05) + 1.0) / 2.0);
            return GradientColorUtils.interpolateAnimation(color1, color2, ratio, animationFactor);
        });
    }

    public static MutableComponent getRainbowGradient(Component text, int offset, int tickrate, boolean reversed) {
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

    private static MutableComponent applyGradient(String text, GradientColorProvider colorProvider) {
        MutableComponent gradientText = Component.empty();

        float maxIndex = Math.max(1.0f, text.length() - 1.0f);

        for (int i = 0; i < text.length(); i++) {
            float ratio = i / maxIndex;
            int color = colorProvider.getColor(i, ratio);
            
            gradientText.append(Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(color)));
        }
        
        return gradientText;
    }
}
