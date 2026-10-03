package com.stalemated.lib.predicate.target;

import net.minecraft.world.item.ItemStack;

public interface TargetMatcher {
    boolean matches(ItemStack stack);
}
