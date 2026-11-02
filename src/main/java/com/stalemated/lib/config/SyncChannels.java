package com.stalemated.lib.config;

import net.minecraft.resources.ResourceLocation;

record SyncChannels(ResourceLocation serverConfig, ResourceLocation clientOverride, ResourceLocation clientInform) {

    static SyncChannels of(ResourceLocation base) {
        return new SyncChannels(child(base, "s2c"), child(base, "override_c2s"), child(base, "inform_c2s"));
    }

    private static ResourceLocation child(ResourceLocation base, String name) {
        String path = base.getPath() + "/" + name;
        //? if <1.21 {
        return new ResourceLocation(base.getNamespace(), path);
        //?} else {
        /*return ResourceLocation.fromNamespaceAndPath(base.getNamespace(), path);
        *///?}
    }
}
