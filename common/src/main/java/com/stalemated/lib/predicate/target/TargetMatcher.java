package com.stalemated.lib.predicate.target;

import net.minecraft.item.ItemStack;

public interface TargetMatcher {
    boolean matches(ItemStack stack);
}