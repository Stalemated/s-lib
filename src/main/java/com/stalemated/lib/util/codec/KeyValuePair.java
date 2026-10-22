package com.stalemated.lib.util.codec;

import com.stalemated.lib.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of a parsed key-value string pair.
 * Provides clean, robust formatting and parsing for "key: value" strings.
 *
 * @param key The key string, never null.
 * @param value The value string, never null.
 */
public record KeyValuePair(String key, String value) {

    public KeyValuePair {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
    }

    /**
     * Formats this pair as a "key: value" string, automatically quoting the key if it
     * contains colons, whitespace, quotes, or backslashes.
     *
     * @return The formatted pair string.
     */
    public String format() {
        if (requiresQuotes(key)) {
            return "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\": " + value;
        }
        return key + ": " + value;
    }

    /**
     * Parses a single "key: value" line into a {@link KeyValuePair}.
     * Supports quoted keys (double or single quotes), unquoted keys with delimiters, and empty values.
     *
     * @param line The string line to parse.
     * @return The parsed {@link KeyValuePair}, or null if the line is null or empty.
     */
    public static KeyValuePair parse(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return null;

        if (trimmed.startsWith("\"") || trimmed.startsWith("'")) {
            return parseQuoted(trimmed);
        }
        return parseUnquoted(trimmed);
    }

    /**
     * Converts a map to a list of formatted "key: value" strings.
     *
     * @param map The source map.
     * @return A list of formatted string pairs, never null.
     */
    public static List<String> toLines(Map<String, ?> map) {
        if (map == null || map.isEmpty()) return new ArrayList<>();
        List<String> result = new ArrayList<>(map.size());

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry == null || entry.getKey() == null) continue;
            String valStr = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            result.add(new KeyValuePair(entry.getKey(), valStr).format());
        }
        return result;
    }

    /**
     * Parses a list of "key: value" strings into a Map preserving encounter order.
     *
     * @param lines The list of formatted strings.
     * @return A map with parsed entries, preserving encounter order.
     */
    public static Map<String, String> fromLines(List<String> lines) {
        Map<String, String> result = new LinkedHashMap<>();
        if (lines == null) return result;

        for (String line : lines) {
            KeyValuePair pair = parse(line);
            if (pair != null) {
                result.put(pair.key(), pair.value());
            }
        }
        return result;
    }

    private static boolean requiresQuotes(String key) {
        return key.contains(":") || key.contains(" ") || key.contains("\"") || key.contains("'") || key.contains("\\");
    }

    private static KeyValuePair parseQuoted(String trimmed) {
        char q = trimmed.charAt(0);
        int end = findClosingQuote(trimmed, q);
        if (end == -1) return parseUnquoted(trimmed);

        String rawKey = trimmed.substring(1, end).replace("\\" + q, String.valueOf(q)).replace("\\\\", "\\").trim();
        if (rawKey.isEmpty()) return parseUnquoted(trimmed);

        String rest = trimmed.substring(end + 1).trim();
        if (rest.startsWith(":")) rest = rest.substring(1).trim();
        return new KeyValuePair(rawKey, StringUtils.stripQuotes(rest));
    }

    private static int findClosingQuote(String trimmed, char quoteChar) {
        for (int i = 1; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) == quoteChar) {
                int backslashes = 0;
                for (int j = i - 1; j >= 0 && trimmed.charAt(j) == '\\'; j--) {
                    backslashes++;
                }
                if (backslashes % 2 == 0) return i;
            }
        }
        return -1;
    }

    private static KeyValuePair parseUnquoted(String trimmed) {
        int idx = trimmed.indexOf(": ");
        int skip = (idx != -1) ? 2 : 1;
        if (idx == -1) idx = trimmed.indexOf(':');

        if (idx != -1 && !trimmed.substring(0, idx).trim().isEmpty()) {
            String key = trimmed.substring(0, idx).trim();
            String val = trimmed.substring(idx + skip).trim();
            return new KeyValuePair(StringUtils.stripQuotes(key), StringUtils.stripQuotes(val));
        }
        return new KeyValuePair(StringUtils.stripQuotes(trimmed), "");
    }
}
