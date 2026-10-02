package com.stalemated.lib.predicate.target.strategies;

import com.stalemated.lib.predicate.target.TargetMatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.ResourceLocation;

public class ItemStrategy implements TargetMatcher {
    private final ResourceLocation itemId;

    public ItemStrategy(String itemId) {
        this.itemId = new ResourceLocation(itemId);
    }

    @Override
    public boolean matches(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).equals(this.itemId);
    }
}
