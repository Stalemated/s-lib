package com.stalemated.lib.predicate.target;

import net.minecraft.world.item.ItemStack;

final class NotStrategy implements TargetMatcher {
    private final TargetMatcher matcher;

    NotStrategy(TargetMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return !matcher.matches(stack);
    }
}
