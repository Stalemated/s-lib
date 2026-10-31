package com.stalemated.lib.compat.yacl.registry;

import com.stalemated.lib.config.model.OptionInfo;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;

@FunctionalInterface
public interface ControllerFactory<V> {
    ControllerBuilder<V> create(OptionInfo info, Option<V> yaclOption);
}
