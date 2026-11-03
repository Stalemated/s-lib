package com.stalemated.lib.util.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RegexText {

    private static final Pattern LEADING_FLAGS = Pattern.compile("\\(\\?([a-zA-Z]+)\\)");

    private RegexText() {}

    public static String format(Pattern regex) {
        StringBuilder letters = new StringBuilder();
        for (RegexFlag flag : RegexFlag.values()) {
            if (!flag.isSetIn(regex.flags())) continue;
            if (!flag.hasInlineLetter()) {
                throw new IllegalArgumentException("Regex '" + regex + "' uses " + flag + ", which has no inline form");
            }
            letters.append(flag.inlineLetter());
        }
        return letters.isEmpty() ? regex.pattern() : "(?" + letters + ")" + regex.pattern();
    }

    public static Pattern parse(String text) {
        Matcher leading = LEADING_FLAGS.matcher(text);
        if (!leading.lookingAt()) {
            return Pattern.compile(text);
        }

        int flags = 0;
        for (char letter : leading.group(1).toCharArray()) {
            flags |= RegexFlag.withInlineLetter(letter).bit();
        }
        return Pattern.compile(text.substring(leading.end()), flags);
    }
}
