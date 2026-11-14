package com.stalemated.lib.compat.yacl.registry;

import com.stalemated.lib.config.OptionInfo;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;

/**
 * Makes the YACL {@link ControllerBuilder} of an option. Register it in
 * {@link YaclControllerRegistry}; it runs for every option and list element that uses it.
 * Client only.
 *
 * @param <V> the value type edited by the YACL option
 */
@FunctionalInterface
public interface ControllerFactory<V> {
    /**
     * Creates the controller builder for {@code yaclOption}.
     *
     * @param info metadata of the option, such as its annotations; for list elements and POJO fields it is a template, so don't call {@code getValue} or {@code setValue} on it
     * @param yaclOption the YACL option whose value this controller edits
     * @return the controller builder, not {@code null}
     */
    ControllerBuilder<V> create(OptionInfo info, Option<V> yaclOption);
}
