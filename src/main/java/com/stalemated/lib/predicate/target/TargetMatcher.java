package com.stalemated.lib.predicate.target;

import net.minecraft.world.item.ItemStack;

/**
 * Matches an {@link ItemStack} against an item target expression. Create one with
 * {@link TargetMatcherFactory}.
 */
public interface TargetMatcher {
    /**
     * Returns whether {@code stack} matches this item target expression. Tag targets only
     * match once the tags are loaded.
     *
     * @param stack the item stack to test, not {@code null}
     * @return {@code true} if {@code stack} matches
     */
    boolean matches(ItemStack stack);
}
