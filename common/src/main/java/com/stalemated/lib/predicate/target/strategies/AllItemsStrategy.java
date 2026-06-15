package com.stalemated.lib.predicate.target.strategies;

import com.stalemated.lib.predicate.target.TargetMatcher;
import net.minecraft.item.ItemStack;

public class AllItemsStrategy implements TargetMatcher {
    @Override
    public boolean matches(ItemStack stack) { return true; }
}