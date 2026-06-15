package com.stalemated.lib.compat.yacl.controller.builder;

import com.stalemated.lib.compat.yacl.controller.SimpleEnumDropdownController;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.ValueFormatter;
import dev.isxander.yacl3.gui.controllers.cycling.EnumController;
import net.minecraft.text.Text;

import java.util.Objects;
import java.util.function.Function;

public class SimpleEnumDropdownControllerBuilder<E extends Enum<E>> implements ControllerBuilder<E> {
    private final Option<E> option;
    private ValueFormatter<E> formatter;

    private SimpleEnumDropdownControllerBuilder(Option<E> option) {
        this.option = option;
        Function<E, Text> defaultFormatter = EnumController.getDefaultFormatter();
        Objects.requireNonNull(defaultFormatter);
        this.formatter = defaultFormatter::apply;
    }

    public static <E extends Enum<E>> SimpleEnumDropdownControllerBuilder<E> create(Option<E> option) {
        return new SimpleEnumDropdownControllerBuilder<>(option);
    }

    public SimpleEnumDropdownControllerBuilder<E> formatValue(ValueFormatter<E> formatter) {
        this.formatter = formatter;
        return this;
    }

    @Override
    public Controller<E> build() {
        return new SimpleEnumDropdownController<>(this.option, this.formatter);
    }
}