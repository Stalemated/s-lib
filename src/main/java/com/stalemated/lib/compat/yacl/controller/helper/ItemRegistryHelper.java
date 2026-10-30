package com.stalemated.lib.compat.yacl.controller.helper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.stream.Stream;

public final class ItemRegistryHelper {

    private ItemRegistryHelper() {}

    public static Stream<String> getTagNames() {
        //? if <1.21.2 {
        return BuiltInRegistries.ITEM.getTagNames()
                .map(tagKey -> "#" + tagKey.location());
        //?} else {
        /*return BuiltInRegistries.ITEM.getTags()
                .map(tag -> "#" + tag.key().location());
        *///?}
    }

    public static Item getItem(ResourceLocation id) {
        //? if <1.21.2 {
        return BuiltInRegistries.ITEM.get(id);
        //?} else {
        /*return BuiltInRegistries.ITEM.getValue(id);
        *///?}
    }

    public static Item getItem(String rawId) {
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        return id != null ? getItem(id) : Items.AIR;
    }

    public static Component getItemName(Item item) {
        //? if <1.21.2 {
        return item.getDescription();
        //?} elif <26.1 {
        /*return item.getName();
        *///?} else {
        /*return item.getName(item.getDefaultInstance());
        *///?}
    }
}
