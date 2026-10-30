package com.stalemated.lib.compat.yacl.controller;

import com.stalemated.lib.compat.yacl.controller.helper.ItemTargetIndex;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownController;

import java.util.List;
import java.util.function.Supplier;

public class ItemOrTagController extends AbstractDropdownController<String> {

    private final Supplier<ItemTargetIndex> targets;

    public ItemOrTagController(Option<String> option, Supplier<ItemTargetIndex> targets) {
        super(option, List.of(), true, true);
        this.targets = targets;
    }

    public ItemTargetIndex targets() {
        return targets.get();
    }

    @Override
    public List<String> getAllowedValues(String inputField) {
        return targets.get().all();
    }

    @Override
    public String getString() {
        return this.option.pendingValue();
    }

    @Override
    public void setFromString(String value) {
        this.option.requestSet(value);
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new ItemOrTagControllerElement(this, screen, widgetDimension);
    }
}
