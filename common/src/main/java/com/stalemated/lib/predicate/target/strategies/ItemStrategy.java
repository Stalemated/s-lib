package com.stalemated.lib.predicate.target.strategies;

import com.stalemated.lib.predicate.target.TargetMatcher;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ItemStrategy implements TargetMatcher {
    private final Item item;
    public ItemStrategy(String itemId) { this.item = Registries.ITEM.get(new Identifier(itemId)); }

    @Override
    public boolean matches(ItemStack stack) {
        if (item == null) return false;
        return stack.isOf(item);
    }
}