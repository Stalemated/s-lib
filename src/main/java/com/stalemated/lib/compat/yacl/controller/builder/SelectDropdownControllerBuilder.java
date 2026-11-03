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

public final class SelectDropdownControllerBuilder<T> implements ControllerBuilder<T> {

    private final Option<T> option;
    private List<T> values = List.of();
    private ValueFormatter<T> formatter = value -> Component.literal(String.valueOf(value));

    private SelectDropdownControllerBuilder(Option<T> option) {
        this.option = Objects.requireNonNull(option, "option cannot be null");
    }

    public static <T> SelectDropdownControllerBuilder<T> create(Option<T> option) {
        return new SelectDropdownControllerBuilder<>(option);
    }

    public static <E extends Enum<E>> SelectDropdownControllerBuilder<E> createEnum(Option<E> option) {
        Function<E, Component> constantNames = EnumController.getDefaultFormatter();
        return create(option)
                .values(List.of(option.pendingValue().getDeclaringClass().getEnumConstants()))
                .formatValue(constantNames::apply);
    }

    public SelectDropdownControllerBuilder<T> values(List<T> values) {
        this.values = List.copyOf(values);
        return this;
    }

    public SelectDropdownControllerBuilder<T> formatValue(ValueFormatter<T> formatter) {
        this.formatter = Objects.requireNonNull(formatter, "formatter cannot be null");
        return this;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public Controller<T> build() {
        return new SelectDropdownController<>(option, values, formatter);
    }
}
