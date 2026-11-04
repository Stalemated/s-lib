package com.stalemated.lib.util;

import net.minecraft.resources.ResourceLocation;

public final class ResourceIds {

    private ResourceIds() {}

    public static ResourceLocation of(String namespace, String path) {
        //? if <1.21 {
        return new ResourceLocation(namespace, path);
        //?} else {
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);
        *///?}
    }

    public static ResourceLocation parse(String id) {
        //? if <1.21 {
        return new ResourceLocation(id);
        //?} else {
        /*return ResourceLocation.parse(id);
        *///?}
    }
}
