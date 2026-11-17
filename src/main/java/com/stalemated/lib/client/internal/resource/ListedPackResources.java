package com.stalemated.lib.client.internal.resource;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//? if >=1.20.5 {
/*import net.minecraft.server.packs.PackLocationInfo;
*///?}
//? if <1.21.4 {
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
//?} else {
/*import net.minecraft.server.packs.metadata.MetadataSectionType;
*///?}

import java.io.InputStream;
import java.util.Set;

final class ListedPackResources implements PackResources {
    //? if <1.20.5 {
    private final String packId;
    //?} else {
    /*private final PackLocationInfo location;
    *///?}
    private final PackListing listing;

    //? if <1.20.5 {
    ListedPackResources(String packId, PackListing listing) {
        this.packId = packId;
        this.listing = listing;
    }
    //?} else {
    /*ListedPackResources(PackLocationInfo location, PackListing listing) {
        this.location = location;
        this.listing = listing;
    }
    *///?}

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String @NotNull ... path) {
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(@NotNull PackType type, @NotNull ResourceLocation location) {
        return type == PackType.CLIENT_RESOURCES ? listing.resource(location) : null;
    }

    @Override
    public void listResources(@NotNull PackType type, @NotNull String namespace, @NotNull String path, @NotNull ResourceOutput output) {
        if (type == PackType.CLIENT_RESOURCES) listing.list(namespace, path, output);
    }

    @Override
    public @NotNull Set<String> getNamespaces(@NotNull PackType type) {
        return type == PackType.CLIENT_RESOURCES ? listing.namespaces() : Set.of();
    }

    //? if <1.21.4 {
    @Override
    public <T> @Nullable T getMetadataSection(@NotNull MetadataSectionSerializer<T> serializer) {
        return null;
    }
    //?} else {
    /*@Override
    public <T> @Nullable T getMetadataSection(@NotNull MetadataSectionType<T> type) {
        return null;
    }
    *///?}

    //? if <1.20.5 {
    @Override
    public @NotNull String packId() {
        return packId;
    }
    //?} else {
    /*@Override
    public @NotNull PackLocationInfo location() {
        return location;
    }
    *///?}

    @Override
    public void close() {
    }
}
