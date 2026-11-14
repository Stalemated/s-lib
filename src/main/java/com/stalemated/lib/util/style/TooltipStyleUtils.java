package com.stalemated.lib.util.style;

import com.stalemated.lib.mixin.internal.client.accessor.OrderedTextTooltipComponentAccessor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import java.util.Optional;

/**
 * Turns {@link FormattedText} and {@link FormattedCharSequence} into a {@link MutableComponent}
 * that keeps the styles, and reads the text of tooltip lines. Client only.
 */
public final class TooltipStyleUtils {

    private TooltipStyleUtils() {}

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

    /**
     * Converts a {@link FormattedText} into a {@link MutableComponent} with one literal
     * sibling per run of text with the same {@link Style}.
     *
     * @param visitable the formatted text to convert
     * @return a styled {@link MutableComponent}
     */
    public static MutableComponent preserveStyles(FormattedText visitable) {
        StyleAccumulator acc = new StyleAccumulator();
        visitable.visit((style, string) -> {
            acc.append(style, string);
            return Optional.empty();
        }, Style.EMPTY);
        return acc.build();
    }

    /**
     * Converts an ordered {@link FormattedCharSequence} into a {@link MutableComponent} with
     * one literal sibling per run of characters with the same {@link Style}.
     *
     * @param orderedText the ordered character sequence
     * @return a styled {@link MutableComponent}
     */
    public static MutableComponent convertOrderedTextToMutable(FormattedCharSequence orderedText) {
        StyleAccumulator acc = new StyleAccumulator();
        orderedText.accept((index, style, codePoint) -> {
            acc.append(style, codePoint);
            return true;
        });
        return acc.build();
    }

    /**
     * Extracts the underlying {@link FormattedCharSequence} from {@code comp} if it is a
     * {@code ClientTextTooltip}, including {@code IndentedTextTooltipComponent}. Needs the
     * S-Lib client mixin.
     *
     * @param comp the tooltip line, or {@code null}
     * @return an {@link Optional} containing the text sequence, or empty if {@code comp} is not
     *         a text line or has no text
     */
    public static Optional<FormattedCharSequence> getExtractedTextValue(ClientTooltipComponent comp) {
        if (comp instanceof OrderedTextTooltipComponentAccessor accessor) {
            return Optional.ofNullable(accessor.getText());
        }
        return Optional.empty();
    }

    /**
     * Extracts the text of a {@link ClientTooltipComponent} without styles.
     *
     * @param comp the tooltip line, or {@code null}
     * @return the text, or {@code ""} if {@code comp} is not a text line
     */
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
