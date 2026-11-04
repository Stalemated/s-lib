package com.stalemated.lib.predicate.target;

import com.stalemated.lib.predicate.target.internal.TagReloads;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class RegexStrategy implements TargetMatcher {
    private final Pattern pattern;
    private final boolean canMatchTag;
    private final Map<Object, Boolean> matchesByItem = new ConcurrentHashMap<>();
    private volatile int cachedGeneration = TagReloads.generation();

    RegexStrategy(String regex) {
        this.pattern = Pattern.compile(regex);
        Matcher probe = pattern.matcher("#");
        this.canMatchTag = probe.matches() || probe.hitEnd();
    }

    @Override
    public boolean matches(ItemStack stack) {
        Item item = stack.getItem();
        return matches(item, () -> BuiltInRegistries.ITEM.getKey(item).toString(), () -> tagIds(stack));
    }

    boolean matches(Object item, Supplier<String> itemId, Supplier<Stream<String>> tagIds) {
        int generation = TagReloads.generation();
        if (generation != cachedGeneration) {
            matchesByItem.clear();
            cachedGeneration = generation;
        }
        Boolean cached = matchesByItem.get(item);
        if (cached != null) return cached;

        boolean matched = pattern.matcher(itemId.get()).matches()
                || canMatchTag && tagIds.get().anyMatch(tag -> pattern.matcher(tag).matches());
        matchesByItem.put(item, matched);
        return matched;
    }

    private static Stream<String> tagIds(ItemStack stack) {
        //? if <26.1 {
        return stack.getTags().map(tag -> "#" + tag.location());
        //?} else {
        /*return stack.typeHolder().tags().map(tag -> "#" + tag.location());
        *///?}
    }
}
