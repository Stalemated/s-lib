package com.stalemated.lib.predicate.target;

import net.minecraft.world.item.ItemStack;

final class AllItemsStrategy implements TargetMatcher {
    @Override
    public boolean matches(ItemStack stack) { return true; }
}
