package com.stalemated.lib.predicate.target.strategies;

import com.stalemated.lib.predicate.target.TargetMatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ItemStrategy implements TargetMatcher {
    private final Identifier itemId;

    public ItemStrategy(String itemId) {
        this.itemId = new Identifier(itemId);
    }

    @Override
    public boolean matches(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).equals(this.itemId);
    }
}