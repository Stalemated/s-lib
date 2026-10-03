package com.stalemated.lib.predicate.target.strategies;

import com.stalemated.lib.predicate.target.TargetMatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public class NamespaceStrategy implements TargetMatcher {
    private final String namespace;
    public NamespaceStrategy(String namespace) { this.namespace = namespace; }

    @Override
    public boolean matches(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(namespace);
    }
}
