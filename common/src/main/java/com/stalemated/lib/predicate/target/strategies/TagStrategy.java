package com.stalemated.lib.predicate.target.strategies;

import com.stalemated.lib.predicate.target.TargetMatcher;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class TagStrategy implements TargetMatcher {
    private final TagKey<Item> tagKey;
    public TagStrategy(String tagId) {
        this.tagKey = TagKey.of(RegistryKeys.ITEM, new Identifier(tagId));
    }

    @Override
    public boolean matches(ItemStack stack) { return stack.isIn(tagKey); }
}
