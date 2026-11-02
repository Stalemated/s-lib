package com.stalemated.lib.util.codec;

import org.intellij.lang.annotations.MagicConstant;

import java.util.Arrays;
import java.util.regex.Pattern;

public enum RegexFlag {
    UNIX_LINES(Pattern.UNIX_LINES, 'd'),
    CASE_INSENSITIVE(Pattern.CASE_INSENSITIVE, 'i'),
    COMMENTS(Pattern.COMMENTS, 'x'),
    MULTILINE(Pattern.MULTILINE, 'm'),
    LITERAL(Pattern.LITERAL),
    DOTALL(Pattern.DOTALL, 's'),
    UNICODE_CASE(Pattern.UNICODE_CASE, 'u'),
    CANON_EQ(Pattern.CANON_EQ),
    UNICODE_CHARACTER_CLASS(Pattern.UNICODE_CHARACTER_CLASS, 'U');

    @MagicConstant(flagsFromClass = Pattern.class)
    private final int bit;
    private final Character inlineLetter;

    RegexFlag(@MagicConstant(flagsFromClass = Pattern.class) int bit) {
        this(bit, null);
    }

    RegexFlag(@MagicConstant(flagsFromClass = Pattern.class)int bit, Character inlineLetter) {
        this.bit = bit;
        this.inlineLetter = inlineLetter;
    }

    @MagicConstant(flagsFromClass = Pattern.class)
    public int bit() {
        return bit;
    }

    public boolean isSetIn(int flags) {
        return (flags & bit) != 0;
    }

    public boolean hasInlineLetter() {
        return inlineLetter != null;
    }

    public char inlineLetter() {
        if (inlineLetter == null) {
            throw new IllegalStateException(this + " has no inline letter");
        }
        return inlineLetter;
    }

    public static RegexFlag named(String name) {
        for (RegexFlag flag : values()) {
            if (flag.name().equals(name)) return flag;
        }
        throw new IllegalArgumentException("Unknown regex flag '" + name + "', expected one of " + Arrays.toString(values()));
    }

    public static RegexFlag withInlineLetter(char letter) {
        for (RegexFlag flag : values()) {
            if (flag.inlineLetter != null && flag.inlineLetter == letter) return flag;
        }
        throw new IllegalArgumentException("Unknown inline regex flag '" + letter + "'");
    }
}
