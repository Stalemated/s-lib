package com.stalemated.lib.compat.yacl.internal;

import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class ItemTargetIndex {

    public static final int MAX_MATCHES = 200;

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
        String needle = input.strip().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return targets.subList(0, Math.min(MAX_MATCHES, targets.size()));
        }

        List<String> prefixed = new ArrayList<>();
        List<String> contained = new ArrayList<>();
        for (String target : targets) {
            if (prefixed.size() == MAX_MATCHES) break;
            if (startsAtWord(target, needle)) {
                prefixed.add(target);
            } else if (contained.size() < MAX_MATCHES && target.contains(needle)) {
                contained.add(target);
            }
        }
        prefixed.addAll(contained.subList(0, Math.min(contained.size(), MAX_MATCHES - prefixed.size())));
        return prefixed;
    }

    private static boolean startsAtWord(String target, String needle) {
        if (target.startsWith(needle)) return true;
        if (target.startsWith("#") && target.startsWith(needle, 1)) return true;
        int separator = target.indexOf(':');
        return separator >= 0 && target.startsWith(needle, separator + 1);
    }
}
