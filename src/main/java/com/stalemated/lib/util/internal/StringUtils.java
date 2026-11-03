package com.stalemated.lib.util.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pure-Java utility class for string manipulation, parsing, and formatting.
 */
public final class StringUtils {

    private static final String QUOTES = "\"'";
    private static final String ESCAPABLE = "\\\"'";

    private StringUtils() {}

    /**
     * Strips enclosing double or single quotes from a string if present, unescaping internal quotes and backslashes.
     * The quotes are only removed when the whole trimmed string is one quoted token.
     *
     * @param s The string to strip quotes from.
     * @return The unquoted string, or the trimmed string if it is not a single quoted token.
     */
    public static String stripQuotes(String s) {
        String trimmed = s.trim();
        if (trimmed.length() < 2 || QUOTES.indexOf(trimmed.charAt(0)) < 0) return trimmed;

        char quote = trimmed.charAt(0);
        StringBuilder content = new StringBuilder(trimmed.length());
        for (int i = 1; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '\\' && i + 1 < trimmed.length() && ESCAPABLE.indexOf(trimmed.charAt(i + 1)) >= 0) {
                content.append(trimmed.charAt(++i));
            } else if (c == quote) {
                return i == trimmed.length() - 1 ? content.toString() : trimmed;
            } else {
                content.append(c);
            }
        }
        return trimmed;
    }

    /**
     * Parses a comma-separated string into a list of trimmed, non-empty tokens.
     *
     * @param val The comma-separated string.
     * @return A list of trimmed tokens, never null.
     */
    public static List<String> parseCommaSeparated(String val) {
        if (val == null || val.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(val.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Joins a collection of elements into a single string separated by commas.
     *
     * @param items The items to join.
     * @return A comma-separated string, or an empty string if null or empty.
     */
    public static String joinCommaSeparated(Collection<?> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }
}
