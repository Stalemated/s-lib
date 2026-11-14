package com.stalemated.lib.compat.yacl.controller.builder;

import com.stalemated.lib.compat.yacl.internal.AdvancedColorController;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;

/**
 * YACL {@link ControllerBuilder} for a color stored as text: a text field plus the YACL color
 * picker. The default controller registry uses it for {@code Color}, with alpha, and for
 * {@code TextColor}, without alpha. Client only.
 */
public class AdvancedColorControllerBuilder implements ControllerBuilder<String> {
    private final Option<String> option;
    private boolean alpha = false;

    private AdvancedColorControllerBuilder(Option<String> option) {
        this.option = option;
    }

    /**
     * Creates a new builder with alpha editing disabled by default.
     *
     * @param option the option that holds the color as text
     * @return a new {@link AdvancedColorControllerBuilder}
     */
    public static AdvancedColorControllerBuilder create(Option<String> option) {
        return new AdvancedColorControllerBuilder(option);
    }

    /**
     * Sets whether the color has an alpha channel.
     * <p>
     * With {@code true}, the field takes {@code AARRGGBB} or {@code RRGGBB} hex, color names and
     * {@code &} codes. With {@code false}, it takes {@code RRGGBB} hex, color names and {@code &}
     * codes. Hex can start with {@code #}, {@code 0x} or {@code x}. Text that is not a color shows
     * in red and is not saved.
     *
     * @param alpha {@code true} to edit the alpha channel
     * @return this builder
     */
    public AdvancedColorControllerBuilder alpha(boolean alpha) {
        this.alpha = alpha;
        return this;
    }

    /**
     * Builds the controller. A color picked in the picker is written as {@code #RRGGBB} when it is
     * opaque, or {@code #AARRGGBB} otherwise. If the option holds text that is not a color,
     * creating its widget throws an {@code IllegalStateException}.
     *
     * @return the built {@link Controller}
     */
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public Controller<String> build() {
        return new AdvancedColorController(this.option, this.alpha);
    }
}
