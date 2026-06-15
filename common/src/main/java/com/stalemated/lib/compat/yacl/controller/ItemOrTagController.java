package com.stalemated.lib.compat.yacl.controller;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownController;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemOrTagController extends AbstractDropdownController<String> {

    public ItemOrTagController(Option<String> option) {
        super(option, getRegistryValues(), true, true);
    }

    private static List<String> getRegistryValues() {
        List<String> values = new ArrayList<>();
        Set<String> namespaces = new HashSet<>();

        values.add("*");

        Registries.ITEM.getIds().forEach(id -> {
            values.add(id.toString());
            namespaces.add(id.getNamespace());
        });

        namespaces.forEach(ns -> values.add(ns + ":*"));

        Registries.ITEM.streamTags()
                .map(tagKey -> "#" + tagKey.id().toString())
                .forEach(values::add);
        
        values.sort(String::compareTo);
        return values;
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