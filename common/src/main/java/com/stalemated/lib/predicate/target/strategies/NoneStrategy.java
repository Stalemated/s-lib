package com.stalemated.lib.predicate.target.strategies;

import com.stalemated.lib.predicate.target.TargetMatcher;
import net.minecraft.item.ItemStack;

public class NoneStrategy implements TargetMatcher {
    @Override
    public boolean matches(ItemStack stack) { return false; }
}
