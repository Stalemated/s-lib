package com.stalemated.lib.compat.yacl.controller.builder;

import com.stalemated.lib.compat.yacl.controller.AdvancedColorController;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;

public class AdvancedColorControllerBuilder implements ControllerBuilder<String> {
    private final Option<String> option;
    private boolean alpha = false;

    private AdvancedColorControllerBuilder(Option<String> option) {
        this.option = option;
    }

    public static AdvancedColorControllerBuilder create(Option<String> option) {
        return new AdvancedColorControllerBuilder(option);
    }

    public AdvancedColorControllerBuilder alpha(boolean alpha) {
        this.alpha = alpha;
        return this;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public Controller<String> build() {
        return new AdvancedColorController(this.option, this.alpha);
    }
}
