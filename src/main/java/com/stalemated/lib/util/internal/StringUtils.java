package com.stalemated.lib.util.internal;

import java.util.ArrayList;
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
     * @return The unquoted string, or the trimmed string if it is not a single-quoted token.
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
     * Parses a comma-separated string into a list of entries. Unquoted entries are trimmed and empty ones are dropped.
     * An entry wrapped in double quotes keeps its commas and edge spaces, and is kept verbatim even when empty.
     * Inside quotes, {@code \"} is a quote and {@code \\} is a backslash; any other backslash is kept as typed.
     *
     * @param val The comma-separated string.
     * @return A list of entries, never null.
     * @throws IllegalArgumentException If a quote is not closed or text follows a closing quote before the next comma.
     */
    public static List<String> parseCommaSeparated(String val) {
        List<String> entries = new ArrayList<>();
        if (val == null) return entries;

        int i = 0;
        while (i < val.length()) {
            while (i < val.length() && val.charAt(i) == ' ') i++;
            if (i < val.length() && val.charAt(i) == '"') {
                StringBuilder quoted = new StringBuilder();
                i = readQuoted(val, i + 1, quoted);
                entries.add(quoted.toString());
                while (i < val.length() && val.charAt(i) == ' ') i++;
                if (i < val.length() && val.charAt(i) != ',') {
                    throw new IllegalArgumentException("list text needs a comma after the quoted entry at position "
                            + i + ", got '" + val.charAt(i) + "' in '" + val + "'");
                }
                i++;
            } else {
                int end = val.indexOf(',', i);
                if (end < 0) end = val.length();
                String plain = val.substring(i, end).trim();
                if (!plain.isEmpty()) entries.add(plain);
                i = end + 1;
            }
        }
        return entries;
    }

    private static int readQuoted(String val, int start, StringBuilder into) {
        for (int i = start; i < val.length(); i++) {
            char c = val.charAt(i);
            if (c == '\\' && i + 1 < val.length() && (val.charAt(i + 1) == '\\' || val.charAt(i + 1) == '"')) {
                into.append(val.charAt(++i));
            } else if (c == '"') {
                return i + 1;
            } else {
                into.append(c);
            }
        }
        throw new IllegalArgumentException("list text has an unclosed quote: '" + val + "'");
    }

    /**
     * Joins a collection of elements into a single string separated by commas. An entry is wrapped in double quotes
     * only when it is empty, contains a comma, starts with a double quote, or has leading or trailing whitespace.
     *
     * @param items The items to join.
     * @return A comma-separated string, or an empty string if null or empty.
     */
    public static String joinCommaSeparated(Collection<?> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(item -> quoteIfNeeded(String.valueOf(item)))
                .collect(Collectors.joining(", "));
    }

    private static String quoteIfNeeded(String entry) {
        boolean needsQuotes = entry.isEmpty() || entry.indexOf(',') >= 0 || entry.charAt(0) == '"'
                || !entry.equals(entry.trim());
        if (!needsQuotes) return entry;

        StringBuilder quoted = new StringBuilder(entry.length() + 2).append('"');
        for (int i = 0; i < entry.length(); i++) {
            char c = entry.charAt(i);
            if (c == '\\' || c == '"') quoted.append('\\');
            quoted.append(c);
        }
        return quoted.append('"').toString();
    }
}
