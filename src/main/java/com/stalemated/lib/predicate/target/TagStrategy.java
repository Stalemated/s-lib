package com.stalemated.lib.predicate.target;

import com.stalemated.lib.util.ResourceIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class TagStrategy implements TargetMatcher {
    private final TagKey<Item> tagKey;
    TagStrategy(String tagId) {
        this.tagKey = TagKey.create(Registries.ITEM, ResourceIds.parse(tagId));
    }

    @Override
    public boolean matches(ItemStack stack) { return stack.is(tagKey); }
}
