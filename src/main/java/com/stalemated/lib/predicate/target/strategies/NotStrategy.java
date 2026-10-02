package com.stalemated.lib.predicate.target.strategies;

import com.stalemated.lib.predicate.target.TargetMatcher;
import net.minecraft.item.ItemStack;

public class NotStrategy implements TargetMatcher {
    private final TargetMatcher matcher;

    public NotStrategy(TargetMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return !matcher.matches(stack);
    }
}
