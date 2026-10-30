package com.stalemated.lib.predicate.target.strategies;

import com.stalemated.lib.predicate.target.TargetMatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.regex.Pattern;

public class NamespaceStrategy implements TargetMatcher {
    private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9_.-]+");

    private final String namespace;

    public NamespaceStrategy(String namespace) {
        if (!VALID_NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("'" + namespace + "' is not a valid namespace");
        }
        this.namespace = namespace;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(namespace);
    }
}
