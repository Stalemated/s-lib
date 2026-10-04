package com.stalemated.lib.predicate.target.strategies;

import com.stalemated.lib.predicate.target.TargetMatcher;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TagStrategy implements TargetMatcher {
    private final TagKey<Item> tagKey;
    public TagStrategy(String tagId) {
        //? if <1.21
        this.tagKey = TagKey.create(Registries.ITEM, new ResourceLocation(tagId));
        //? if >=1.21
        //this.tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId));
    }

    @Override
    public boolean matches(ItemStack stack) { return stack.is(tagKey); }
}
