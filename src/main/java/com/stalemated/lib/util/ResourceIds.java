package com.stalemated.lib.util;

import net.minecraft.resources.ResourceLocation;

/**
 * Helpers for creating and checking {@link ResourceLocation} ids.
 */
public final class ResourceIds {

    private ResourceIds() {}

    /**
     * Creates a {@link ResourceLocation} from a namespace and path.
     *
     * @param namespace the resource namespace
     * @param path the resource path
     * @return the constructed {@link ResourceLocation}
     * @throws net.minecraft.ResourceLocationException if {@code namespace} or {@code path} has
     *                                                 invalid characters
     */
    public static ResourceLocation of(String namespace, String path) {
        //? if <1.21 {
        return new ResourceLocation(namespace, path);
        //?} else {
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);
        *///?}
    }

    /**
     * Parses a {@code namespace:path} string into a {@link ResourceLocation}. Without a
     * namespace it uses {@code minecraft}.
     *
     * @param id the id text
     * @return the parsed {@link ResourceLocation}
     * @throws net.minecraft.ResourceLocationException if {@code id} is not a valid id
     */
    public static ResourceLocation parse(String id) {
        //? if <1.21 {
        return new ResourceLocation(id);
        //?} else {
        /*return ResourceLocation.parse(id);
        *///?}
    }

    /**
     * Returns whether {@code text} is a syntactically valid {@link ResourceLocation}.
     *
     * @param text the text to check
     * @return {@code true} if {@code text} can be parsed as a {@link ResourceLocation}
     */
    public static boolean isValid(String text) {
        return ResourceLocation.tryParse(text) != null;
    }
}
