package com.stalemated.lib.predicate.target;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class TagStrategy implements TargetMatcher {
    private final TagKey<Item> tagKey;
    TagStrategy(String tagId) {
        //? if <1.21
        this.tagKey = TagKey.create(Registries.ITEM, new ResourceLocation(tagId));
        //? if >=1.21
        //this.tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId));
    }

    @Override
    public boolean matches(ItemStack stack) { return stack.is(tagKey); }
}
