package com.stalemated.lib.predicate.target;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class ItemStrategy implements TargetMatcher {
    private final ResourceLocation itemId;

    ItemStrategy(String itemId) {
        //? if <1.21
        this.itemId = new ResourceLocation(itemId);
        //? if >=1.21
        //this.itemId = ResourceLocation.parse(itemId);
    }

    @Override
    public boolean matches(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(this.itemId);
    }
}
