package com.stalemated.lib.compat.yacl.builder;

import com.google.common.collect.ImmutableList;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * Lightweight, non-collapsible {@link OptionGroup} that renders its options inline without a
 * collapsible arrow header bar in YACL, such as a section label or the Add Entry controls of a nested section.
 *
 * @param options The immutable list of options shown under the section.
 */
record YaclSectionHeaderGroup(ImmutableList<Option<?>> options) implements OptionGroup {

    static YaclSectionHeaderGroup of(Component labelText) {
        Objects.requireNonNull(labelText, "labelText cannot be null");
        return new YaclSectionHeaderGroup(ImmutableList.of(LabelOption.create(labelText)));
    }

    @Override
    public Component name() {
        return Component.empty();
    }

    @Override
    public OptionDescription description() {
        return OptionDescription.EMPTY;
    }

    @Override
    @Deprecated
    public Component tooltip() {
        return Component.empty();
    }

    @Override
    public boolean collapsed() {
        return false;
    }

    @Override
    public boolean isRoot() {
        return true;
    }
}
