package com.stalemated.lib.compat.yacl.controller.builder;

import com.stalemated.lib.compat.yacl.controller.ItemOrTagController;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;

public class ItemOrTagControllerBuilder implements ControllerBuilder<String> {
    private final Option<String> option;

    private ItemOrTagControllerBuilder(Option<String> option) {
        this.option = option;
    }

    public static ItemOrTagControllerBuilder create(Option<String> option) {
        return new ItemOrTagControllerBuilder(option);
    }

    @Override
    public Controller<String> build() {
        return new ItemOrTagController(this.option);
    }
}