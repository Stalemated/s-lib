package com.stalemated.lib.compat.yacl.controller.builder;

import com.stalemated.lib.compat.yacl.controller.SimpleStringDropdownController;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.ValueFormatter;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Objects;

public class SimpleStringDropdownControllerBuilder implements ControllerBuilder<String> {
    private final Option<String> option;
    private ValueFormatter<String> formatter = Text::literal;
    private List<String> values;

    private SimpleStringDropdownControllerBuilder(Option<String> option) {
        this.option = option;
    }

    public static SimpleStringDropdownControllerBuilder create(Option<String> option) {
        return new SimpleStringDropdownControllerBuilder(option);
    }

    public SimpleStringDropdownControllerBuilder values(List<String> values) {
        this.values = values;
        return this;
    }

    public SimpleStringDropdownControllerBuilder formatValue(ValueFormatter<String> formatter) {
        this.formatter = formatter;
        return this;
    }

    @Override
    public Controller<String> build() {
        Objects.requireNonNull(values, "List cannot be Null.");
        return new SimpleStringDropdownController(this.option, this.values, this.formatter);
    }
}