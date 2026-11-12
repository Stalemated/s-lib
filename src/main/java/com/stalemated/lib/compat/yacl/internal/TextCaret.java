package com.stalemated.lib.compat.yacl.internal;

import java.util.function.ToIntFunction;

final class TextCaret {

    private TextCaret() {
    }

    static int indexAt(String text, ToIntFunction<String> widthOf, int x) {
        int widthBefore = 0;
        for (int index = 0; index < text.length(); index++) {
            int charWidth = widthOf.applyAsInt(String.valueOf(text.charAt(index)));
            if (widthBefore + charWidth / 2 > x) return index;
            widthBefore += charWidth;
        }
        return text.length();
    }
}
