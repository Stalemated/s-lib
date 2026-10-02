package com.stalemated.lib.util.color;

import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ColorUtils {

    public static final int DEFAULT_COLOR = 0xFFFFFF;
    public static final int DEFAULT_OPACITY = 240;
    public static final List<Integer> DEFAULT_BORDER_COLORS = new ArrayList<>(List.of(0x505000FF, 0x5028007F));
    public static final List<Integer> DEFAULT_BACKGROUND_COLORS = new ArrayList<>(List.of(0xF0100010, 0xF0100010));
    public static final String DEFAULT_COLOR_STRING = "#" + Integer.toHexString(DEFAULT_COLOR).toUpperCase();
    public static final List<String> DEFAULT_BORDER_COLORS_STRING = new ArrayList<>(List.of("#" + Integer.toHexString(DEFAULT_BORDER_COLORS.get(0)).toUpperCase(), "#" + Integer.toHexString(DEFAULT_BORDER_COLORS.get(1)).toUpperCase()));
    public static final List<String> DEFAULT_BACKGROUND_COLORS_STRING = new ArrayList<>(List.of("#" + Integer.toHexString(DEFAULT_BACKGROUND_COLORS.get(0)).toUpperCase(), "#" + Integer.toHexString(DEFAULT_BACKGROUND_COLORS.get(1)).toUpperCase()));

    public static String toHexString(Color color) {
        if (color == null) return DEFAULT_COLOR_STRING;

        if (color.getAlpha() == 255) {
            return String.format("#%06X", color.getRGB() & 0xFFFFFF);
        }
        return String.format("#%08X", color.getRGB());
    }

    public static String toRGBAHexString(Color color) {
        if (color == null) return DEFAULT_COLOR_STRING;

        if (color.getAlpha() == 255) {
            return String.format("#%06X", color.getRGB() & 0xFFFFFF);
        }
        return String.format("#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public static String toHexString(TextColor color) {
        if (color == null) return DEFAULT_COLOR_STRING;
        return color.getName();
    }

    public static TextColor resolveTextColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) return null;

        String lowerColor = colorStr.toLowerCase(Locale.ROOT);

        // Legacy code (e.g., "&c")
        if (lowerColor.length() == 2 && lowerColor.charAt(0) == '&') {
            Formatting format = Formatting.byCode(lowerColor.charAt(1));
            if (format != null && format.getColorValue() != null) {
                return TextColor.fromFormatting(format);
            }
        }

        // Formatting name (e.g., "red")
        Formatting format = Formatting.byName(lowerColor);
        if (format != null && format.getColorValue() != null) {
            return TextColor.fromFormatting(format);
        }

        // Hex code
        String hex = colorStr;
        if (hex.startsWith("#")) hex = hex.substring(1);
        else if (hex.startsWith("0x") || hex.startsWith("0X")) hex = hex.substring(2);
        else if (hex.startsWith("x") || hex.startsWith("X")) hex = hex.substring(1);

        if (hex.matches("^[0-9a-fA-F]{6}$")) {
            return TextColor.parse("#" + hex);
        }

        return null;
    }

    public static boolean isInvalidColorCode(String color) {
        return resolveTextColor(color) == null;
    }

    public static Color parseToAWT(String colorStr) {
        Integer argb = resolveARGBColor(colorStr);
        return argb != null ? new Color(argb, true) : Color.WHITE;
    }

    public static int parseColor(String colorStr) {
        TextColor color = resolveTextColor(colorStr);
        return color != null ? color.getRgb() : DEFAULT_COLOR;
    }

    public static boolean isInvalidARGBColor(String color, int index) {
        return parseARGBColor(color, index) == null;
    }

    public static Integer parseARGBColor(String colorStr, int index) {
        Integer color = resolveARGBColor(colorStr);
        return color != null ? color : DEFAULT_BORDER_COLORS.get(index);
    }

    public static Integer resolveARGBColor(String colorStr) {
        return resolveColor(colorStr, true);
    }

    public static Color parseRGBAToAWT(String colorStr) {
        Integer argb = resolveRGBAColor(colorStr);
        return argb != null ? new Color(argb, true) : Color.WHITE;
    }

    public static Integer resolveRGBAColor(String colorStr) {
        return resolveColor(colorStr, false);
    }

    private static Integer resolveColor(String colorStr, boolean isARGB) {
        if (colorStr == null || colorStr.trim().isEmpty()) return null;

        String lowerColor = colorStr.trim().toLowerCase(Locale.ROOT);

        String hex = colorStr.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        else if (hex.startsWith("0x") || hex.startsWith("0X")) hex = hex.substring(2);
        else if (hex.startsWith("x") || hex.startsWith("X")) hex = hex.substring(1);

        if (hex.matches("^[0-9a-fA-F]{8}$")) {
            if (isARGB) return (int) Long.parseLong(hex, 16);
            long val = Long.parseLong(hex, 16);
            int r = (int) ((val >> 24) & 0xFF);
            int g = (int) ((val >> 16) & 0xFF);
            int b = (int) ((val >> 8) & 0xFF);
            int a = (int) (val & 0xFF);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        if (hex.matches("^[0-9a-fA-F]{6}$")) {
            return (0xFF << 24) | Integer.parseInt(hex, 16);
        }

        TextColor textColor = resolveTextColor(lowerColor);
        if (textColor != null) {
            return (0xFF << 24) | textColor.getRgb();
        }

        return null;
    }
}
