package com.stalemated.lib.compat.yacl.controller.builder;

import com.google.common.base.Suppliers;
import com.stalemated.lib.compat.yacl.internal.ItemOrTagController;
import com.stalemated.lib.compat.yacl.internal.ItemTargetIndex;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;

import java.util.Objects;

public final class ItemOrTagControllerBuilder implements ControllerBuilder<String> {
    private final Option<String> option;
    private boolean allowTags = true;

    private ItemOrTagControllerBuilder(Option<String> option) {
        this.option = Objects.requireNonNull(option, "option cannot be null");
    }

    public static ItemOrTagControllerBuilder create(Option<String> option) {
        return new ItemOrTagControllerBuilder(option);
    }

    public ItemOrTagControllerBuilder allowTags(boolean allowTags) {
        this.allowTags = allowTags;
        return this;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public Controller<String> build() {
        boolean includeTags = allowTags;
        return new ItemOrTagController(option, Suppliers.memoize(() -> ItemTargetIndex.fromRegistry(includeTags)));
    }
}
