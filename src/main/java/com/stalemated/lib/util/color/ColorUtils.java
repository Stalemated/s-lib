package com.stalemated.lib.util.color;

import java.awt.Color;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;

public final class ColorUtils {

    private static final Pattern HEX_RGB = Pattern.compile("[0-9a-fA-F]{6}");
    private static final Pattern HEX_ARGB = Pattern.compile("[0-9a-fA-F]{8}");

    private ColorUtils() {}

    public static String toArgbHex(Color color) {
        if (color.getAlpha() == 255) {
            return String.format("#%06X", color.getRGB() & 0xFFFFFF);
        }
        return String.format("#%08X", color.getRGB());
    }

    public static String toRgbaHex(Color color) {
        if (color.getAlpha() == 255) {
            return String.format("#%06X", color.getRGB() & 0xFFFFFF);
        }
        return String.format("#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public static String toHex(TextColor color) {
        return color.serialize();
    }

    public static TextColor parseTextColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) return null;

        String lowerColor = colorStr.toLowerCase(Locale.ROOT);

        // Legacy code (e.g., "&c")
        if (lowerColor.length() == 2 && lowerColor.charAt(0) == '&') {
            ChatFormatting format = ChatFormatting.getByCode(lowerColor.charAt(1));
            //? if <26.2 {
            if (format != null && format.getColor() != null) {
                return TextColor.fromLegacyFormat(format);
            }
            //?} else {
            /*if (format != null) {
                TextColor color = TextColor.fromLegacyFormat(format);
                if (color != null) return color;
            }
            *///?}
        }

        // Formatting name (e.g., "red")
        //? if <26.2 {
        ChatFormatting format = ChatFormatting.getByName(lowerColor);
        if (format != null && format.getColor() != null) {
            return TextColor.fromLegacyFormat(format);
        }
        //?} else {
        /*TextColor namedColor = TextColor.parseColor(lowerColor).result().orElse(null);
        if (namedColor != null) {
            return namedColor;
        }
        *///?}

        String hex = stripHexPrefix(colorStr);
        if (HEX_RGB.matcher(hex).matches()) {
            return TextColor.fromRgb(Integer.parseInt(hex, 16));
        }

        return null;
    }

    public static Integer parseArgb(String colorStr) {
        return parseHex(colorStr, true);
    }

    public static Integer parseRgba(String colorStr) {
        return parseHex(colorStr, false);
    }

    public static Color parseArgbColor(String colorStr) {
        Integer argb = parseArgb(colorStr);
        return argb != null ? new Color(argb, true) : null;
    }

    private static Integer parseHex(String colorStr, boolean isARGB) {
        if (colorStr == null || colorStr.trim().isEmpty()) return null;

        String lowerColor = colorStr.trim().toLowerCase(Locale.ROOT);
        String hex = stripHexPrefix(colorStr.trim());

        if (HEX_ARGB.matcher(hex).matches()) {
            if (isARGB) return (int) Long.parseLong(hex, 16);
            long val = Long.parseLong(hex, 16);
            int r = (int) ((val >> 24) & 0xFF);
            int g = (int) ((val >> 16) & 0xFF);
            int b = (int) ((val >> 8) & 0xFF);
            int a = (int) (val & 0xFF);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        if (HEX_RGB.matcher(hex).matches()) {
            return (0xFF << 24) | Integer.parseInt(hex, 16);
        }

        TextColor textColor = parseTextColor(lowerColor);
        if (textColor != null) {
            return (0xFF << 24) | textColor.getValue();
        }

        return null;
    }

    private static String stripHexPrefix(String text) {
        if (text.startsWith("#")) return text.substring(1);
        if (text.startsWith("0x") || text.startsWith("0X")) return text.substring(2);
        if (text.startsWith("x") || text.startsWith("X")) return text.substring(1);
        return text;
    }
}
