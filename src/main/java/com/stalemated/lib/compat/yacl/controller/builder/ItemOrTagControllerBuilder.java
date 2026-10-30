package com.stalemated.lib.compat.yacl.controller.builder;

import com.stalemated.lib.compat.yacl.controller.ItemOrTagController;
import com.stalemated.lib.compat.yacl.controller.helper.ItemTargetIndex;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;

import java.util.function.Supplier;

public class ItemOrTagControllerBuilder implements ControllerBuilder<String> {
    private final Option<String> option;
    private final Supplier<ItemTargetIndex> targets;

    private ItemOrTagControllerBuilder(Option<String> option, Supplier<ItemTargetIndex> targets) {
        this.option = option;
        this.targets = targets;
    }

    public static ItemOrTagControllerBuilder create(Option<String> option, Supplier<ItemTargetIndex> targets) {
        return new ItemOrTagControllerBuilder(option, targets);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public Controller<String> build() {
        return new ItemOrTagController(this.option, this.targets);
    }
}
