package com.stalemated.lib.predicate.target.strategies;

import com.stalemated.lib.predicate.target.TargetMatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.regex.Pattern;

public class RegexStrategy implements TargetMatcher {
    private final Pattern pattern;

    public RegexStrategy(String regex) { this.pattern = Pattern.compile(regex); }

    @Override
    public boolean matches(ItemStack stack) {
        if (pattern.matcher(Registries.ITEM.getId(stack.getItem()).toString()).matches()) {
            return true;
        }

        return stack.streamTags().anyMatch(tag -> pattern.matcher("#" + tag.id().toString()).matches());
    }
}