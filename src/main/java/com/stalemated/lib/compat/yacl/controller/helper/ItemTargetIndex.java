package com.stalemated.lib.compat.yacl.controller.helper;

import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class ItemTargetIndex {

    private final List<String> targets;

    public ItemTargetIndex(List<String> targets) {
        this.targets = targets.stream().sorted().toList();
    }

    public static ItemTargetIndex fromRegistry(boolean includeTags) {
        List<String> targets = new ArrayList<>();
        Set<String> namespaces = new TreeSet<>();
        for (var id : BuiltInRegistries.ITEM.keySet()) {
            targets.add(id.toString());
            namespaces.add(id.getNamespace());
        }

        if (includeTags) {
            targets.add("*");
            namespaces.forEach(namespace -> targets.add(namespace + ":*"));
            ItemRegistryHelper.getTagNames().forEach(targets::add);
        }
        return new ItemTargetIndex(targets);
    }

    public List<String> all() {
        return targets;
    }

    public List<String> matching(String input) {
        String needle = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String target : targets) {
            if (target.contains(needle)) {
                matches.add(target);
            }
        }
        return matches;
    }
}
