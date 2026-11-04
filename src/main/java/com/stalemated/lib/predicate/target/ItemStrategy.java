package com.stalemated.lib.predicate.target;

import com.stalemated.lib.util.ResourceIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class ItemStrategy implements TargetMatcher {
    private final ResourceLocation itemId;

    ItemStrategy(String itemId) {
        this.itemId = ResourceIds.parse(itemId);
    }

    @Override
    public boolean matches(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(this.itemId);
    }
}
