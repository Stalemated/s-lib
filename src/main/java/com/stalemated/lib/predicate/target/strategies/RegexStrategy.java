package com.stalemated.lib.predicate.target.strategies;

import com.stalemated.lib.predicate.target.TargetMatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.regex.Pattern;

public class RegexStrategy implements TargetMatcher {
    private final Pattern pattern;

    public RegexStrategy(String regex) { this.pattern = Pattern.compile(regex); }

    @Override
    public boolean matches(ItemStack stack) {
        if (pattern.matcher(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).matches()) {
            return true;
        }

        //? if <26.1 {
        return stack.getTags().anyMatch(tag -> pattern.matcher("#" + tag.location().toString()).matches());
        //?} else {
        /*return stack.typeHolder().tags().anyMatch(tag -> pattern.matcher("#" + tag.location().toString()).matches());
        *///?}
    }
}
