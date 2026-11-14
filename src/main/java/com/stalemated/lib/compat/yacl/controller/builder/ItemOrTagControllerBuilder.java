package com.stalemated.lib.compat.yacl.controller.builder;

import com.google.common.base.Suppliers;
import com.stalemated.lib.compat.yacl.internal.ItemOrTagController;
import com.stalemated.lib.compat.yacl.internal.ItemTargetIndex;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;

import java.util.Objects;

/**
 * YACL {@link ControllerBuilder} for a text field with an item dropdown, the same one that
 * {@code @ItemDropdown} uses. It suggests item ids and, with tags allowed, item tags
 * ({@code #ns:tag}), {@code *} and {@code ns:*}. Any text is accepted, including
 * {@code regex:} patterns and {@code !} negations, and each kind gets its own color.
 * Client only.
 * <p>
 * The suggestions are read from the item registry, and from the loaded tags, the first time
 * the dropdown asks for them.
 */
public final class ItemOrTagControllerBuilder implements ControllerBuilder<String> {
    private final Option<String> option;
    private boolean allowTags = true;

    private ItemOrTagControllerBuilder(Option<String> option) {
        this.option = Objects.requireNonNull(option, "option cannot be null");
    }

    /**
     * Creates a new builder for a {@code String} option, with tag and wildcard suggestions
     * enabled by default.
     *
     * @param option the option to edit
     * @return a new {@link ItemOrTagControllerBuilder}
     * @throws NullPointerException if {@code option} is {@code null}
     */
    public static ItemOrTagControllerBuilder create(Option<String> option) {
        return new ItemOrTagControllerBuilder(option);
    }

    /**
     * Sets whether the suggestions include item tags, {@code *} and {@code ns:*}. The default is
     * {@code true}, as in {@code @ItemDropdown}.
     * <p>
     * It only changes the suggestions: the field accepts any text.
     *
     * @param allowTags {@code false} to suggest item ids only
     * @return this builder
     */
    public ItemOrTagControllerBuilder allowTags(boolean allowTags) {
        this.allowTags = allowTags;
        return this;
    }

    /**
     * Builds the controller. It shows up to 200 suggestions: first the exact match, then the ones
     * that start with the text at a word boundary, then the ones that contain it.
     *
     * @return the built {@link Controller}
     */
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public Controller<String> build() {
        boolean includeTags = allowTags;
        return new ItemOrTagController(option, Suppliers.memoize(() -> ItemTargetIndex.fromRegistry(includeTags)));
    }
}
