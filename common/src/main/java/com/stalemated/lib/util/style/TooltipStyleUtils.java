package com.stalemated.lib.util.style;

import com.stalemated.lib.mixin.client.accessor.OrderedTextTooltipComponentAccessor;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.*;

import java.util.Optional;

public class TooltipStyleUtils {
    private static class StyleAccumulator {
        private final MutableText result = Text.empty();
        private final StringBuilder currentText = new StringBuilder();
        private Style currentStyle = Style.EMPTY;

        void append(Style style, String text) {
            flushIfStyleChanged(style);
            currentText.append(text);
        }

        void append(Style style, int codePoint) {
            flushIfStyleChanged(style);
            currentText.appendCodePoint(codePoint);
        }

        private void flushIfStyleChanged(Style newStyle) {
            if (!newStyle.equals(currentStyle) && !currentText.isEmpty()) {
                result.append(Text.literal(currentText.toString()).setStyle(currentStyle));
                currentText.setLength(0);
            }
            currentStyle = newStyle;
        }

        MutableText build() {
            if (!currentText.isEmpty()) {
                result.append(Text.literal(currentText.toString()).setStyle(currentStyle));
                currentText.setLength(0);
            }
            return result;
        }
    }

    public static MutableText preserveStyles(StringVisitable visitable) {
        StyleAccumulator acc = new StyleAccumulator();
        visitable.visit((style, string) -> {
            acc.append(style, string);
            return Optional.empty();
        }, Style.EMPTY);
        return acc.build();
    }

    public static MutableText convertOrderedTextToMutable(OrderedText orderedText) {
        StyleAccumulator acc = new StyleAccumulator();
        orderedText.accept((index, style, codePoint) -> {
            acc.append(style, codePoint);
            return true;
        });
        return acc.build();
    }

    public static Optional<OrderedText> getExtractedTextValue(TooltipComponent comp) {
        if (comp instanceof OrderedTextTooltipComponentAccessor accessor) {
            return Optional.ofNullable(accessor.getText());
        }
        return Optional.empty();
    }

    public static String getComponentString(TooltipComponent comp) {
        StringBuilder sb = new StringBuilder();
        Optional<OrderedText> extracted = getExtractedTextValue(comp);

        extracted.ifPresent(value -> value.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        }));
        return sb.toString();
    }
}
