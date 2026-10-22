package com.stalemated.lib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pure-Java utility class for string manipulation, parsing, and formatting.
 */
public final class StringUtils {

    private StringUtils() {}

    /**
     * Strips enclosing double or single quotes from a string if present, unescaping internal quotes and backslashes.
     *
     * @param s The string to strip quotes from.
     * @return The unquoted string, or an empty string if null.
     */
    public static String stripQuotes(String s) {
        if (s == null) return "";
        String trimmed = s.trim();
        if (trimmed.length() >= 2) {
            if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
                return trimmed.substring(1, trimmed.length() - 1)
                        .replace("\\\"", "\"")
                        .replace("\\'", "'")
                        .replace("\\\\", "\\");
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
