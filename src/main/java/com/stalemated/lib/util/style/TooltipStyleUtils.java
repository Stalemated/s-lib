package com.stalemated.lib.util.style;

import com.stalemated.lib.mixin.client.accessor.OrderedTextTooltipComponentAccessor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import java.util.Optional;

public class TooltipStyleUtils {
    private static class StyleAccumulator {
        private final MutableComponent result = Component.empty();
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
                result.append(Component.literal(currentText.toString()).setStyle(currentStyle));
                currentText.setLength(0);
            }
            currentStyle = newStyle;
        }

        MutableComponent build() {
            if (!currentText.isEmpty()) {
                result.append(Component.literal(currentText.toString()).setStyle(currentStyle));
                currentText.setLength(0);
            }
            return result;
        }
    }

    public static MutableComponent preserveStyles(FormattedText visitable) {
        StyleAccumulator acc = new StyleAccumulator();
        visitable.visit((style, string) -> {
            acc.append(style, string);
            return Optional.empty();
        }, Style.EMPTY);
        return acc.build();
    }

    public static MutableComponent convertOrderedTextToMutable(FormattedCharSequence orderedText) {
        StyleAccumulator acc = new StyleAccumulator();
        orderedText.accept((index, style, codePoint) -> {
            acc.append(style, codePoint);
            return true;
        });
        return acc.build();
    }

    public static Optional<FormattedCharSequence> getExtractedTextValue(ClientTooltipComponent comp) {
        if (comp instanceof OrderedTextTooltipComponentAccessor accessor) {
            return Optional.ofNullable(accessor.getText());
        }
        return Optional.empty();
    }

    public static String getComponentString(ClientTooltipComponent comp) {
        StringBuilder sb = new StringBuilder();
        Optional<FormattedCharSequence> extracted = getExtractedTextValue(comp);

        extracted.ifPresent(value -> value.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        }));
        return sb.toString();
    }
}
