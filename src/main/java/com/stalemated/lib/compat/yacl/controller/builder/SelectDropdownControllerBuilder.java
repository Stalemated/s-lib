package com.stalemated.lib.compat.yacl.controller.builder;

import com.stalemated.lib.compat.yacl.internal.SelectDropdownController;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.ValueFormatter;
import dev.isxander.yacl3.gui.controllers.cycling.EnumController;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * YACL {@link ControllerBuilder} for a dropdown that picks one value from a fixed list. Each
 * value is shown with a {@link ValueFormatter}; the player can't type in it. Client only.
 *
 * @param <T> the option value type
 */
public final class SelectDropdownControllerBuilder<T> implements ControllerBuilder<T> {

    private final Option<T> option;
    private List<T> values = List.of();
    private ValueFormatter<T> formatter = value -> Component.literal(String.valueOf(value));

    private SelectDropdownControllerBuilder(Option<T> option) {
        this.option = Objects.requireNonNull(option, "option cannot be null");
    }

    /**
     * Creates a new dropdown builder with an empty value list and {@code String.valueOf} formatting.
     *
     * @param option the option to edit
     * @param <T> the option value type
     * @return a new {@link SelectDropdownControllerBuilder}
     * @throws NullPointerException if {@code option} is {@code null}
     */
    public static <T> SelectDropdownControllerBuilder<T> create(Option<T> option) {
        return new SelectDropdownControllerBuilder<>(option);
    }

    /**
     * Creates a builder with every constant of the enum of {@code option}, shown with the
     * default enum formatter of YACL. The enum class comes from the pending value of the option.
     *
     * @param option an enum option whose pending value is not {@code null}
     * @param <E> the enum type
     * @return a new {@link SelectDropdownControllerBuilder}
     * @throws NullPointerException if {@code option} or its pending value is {@code null}
     */
    public static <E extends Enum<E>> SelectDropdownControllerBuilder<E> createEnum(Option<E> option) {
        Function<E, Component> constantNames = EnumController.getDefaultFormatter();
        return create(option)
                .values(List.of(option.pendingValue().getDeclaringClass().getEnumConstants()))
                .formatValue(constantNames::apply);
    }

    /**
     * Sets the values to pick from. The list is copied.
     *
     * @param values the values; none can be {@code null}
     * @return this builder
     * @throws NullPointerException if {@code values} or one of its elements is {@code null}
     */
    public SelectDropdownControllerBuilder<T> values(List<T> values) {
        this.values = List.copyOf(values);
        return this;
    }

    /**
     * Sets the text shown for each value, in the list and in the field.
     *
     * @param formatter makes the text of a value
     * @return this builder
     * @throws NullPointerException if {@code formatter} is {@code null}
     */
    public SelectDropdownControllerBuilder<T> formatValue(ValueFormatter<T> formatter) {
        this.formatter = Objects.requireNonNull(formatter, "formatter cannot be null");
        return this;
    }

    /**
     * Builds the controller. The field always shows the current value, even when it is not in
     * the list.
     *
     * @return the built {@link Controller}
     */
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public Controller<T> build() {
        return new SelectDropdownController<>(option, values, formatter);
    }
}
