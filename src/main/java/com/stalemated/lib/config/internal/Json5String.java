package com.stalemated.lib.config.internal;

import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonPrimitive;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class Json5String extends JsonPrimitive {
    private static final Map<Integer, String> SHORT_ESCAPES = Map.of(
            (int) '"', "\\\"",
            (int) '\\', "\\\\",
            (int) '\n', "\\n",
            (int) '\r', "\\r",
            (int) '\t', "\\t",
            (int) '\b', "\\b",
            (int) '\f', "\\f");
    private static final Set<Integer> ESCAPED_TYPES = Set.of(
            (int) Character.CONTROL,
            (int) Character.SURROGATE,
            (int) Character.LINE_SEPARATOR,
            (int) Character.PARAGRAPH_SEPARATOR);
    private static final Set<Integer> ESCAPED_CODE_POINTS = Stream.of(
                    IntStream.of(0x061C, 0x200B, 0x200E, 0x200F, 0xFEFF, 0xFFFF),
                    IntStream.rangeClosed(0x202A, 0x202E),
                    IntStream.rangeClosed(0x2060, 0x2064),
                    IntStream.rangeClosed(0x2066, 0x2069))
            .flatMapToInt(codePoints -> codePoints)
            .boxed()
            .collect(Collectors.toUnmodifiableSet());

    Json5String(String text) {
        super(text);
    }

    static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        text.codePoints().forEach(codePoint -> {
            String shortEscape = SHORT_ESCAPES.get(codePoint);
            if (shortEscape != null) {
                escaped.append(shortEscape);
            } else if (ESCAPED_TYPES.contains(Character.getType(codePoint)) || ESCAPED_CODE_POINTS.contains(codePoint)) {
                escaped.append(String.format(Locale.ROOT, "\\u%04x", codePoint));
            } else {
                escaped.appendCodePoint(codePoint);
            }
        });
        return escaped.toString();
    }

    @Override
    public void toJson(Writer writer, JsonGrammar grammar, int depth) throws IOException {
        writer.write('"');
        writer.write(escape(asString()));
        writer.write('"');
    }
}
