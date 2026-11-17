package com.stalemated.lib.client.internal.resource;

import com.stalemated.lib.client.internal.resource.ClientPackRegistry.RegisteredPack;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jetbrains.annotations.NotNull;
//? if <1.20.2 {
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;
//?} elif <1.20.5 {
/*import net.minecraft.server.packs.repository.PackCompatibility;
import java.util.List;
*///?} else {
/*import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.PackCompatibility;
import java.util.List;
import java.util.Optional;
*///?}
//? if >=26.3 {
/*import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.PackResources;
import java.util.stream.Stream;
*///?}

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import static com.stalemated.lib.SLib.LOGGER;

public final class RegisteredClientPackSource implements RepositorySource {

    public static Set<RepositorySource> withClientPacks(Set<RepositorySource> sources) {
        if (sources.stream().noneMatch(ClientPackSource.class::isInstance)) return sources;
        Set<RepositorySource> withClientPacks = new LinkedHashSet<>(sources);
        withClientPacks.add(new RegisteredClientPackSource());
        return withClientPacks;
    }

    @Override
    public void loadPacks(@NotNull Consumer<Pack> packs) {
        ClientPackRegistry.all().forEach(registered -> packs.accept(packOf(registered)));
    }

    private static Pack packOf(RegisteredPack registered) {
        String id = registered.id().toString();
        //? if <1.20.2 {
        Pack.Info info = new Pack.Info(registered.title(), SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES), FeatureFlagSet.of());
        return Pack.create(id, registered.title(), true, packId -> new ListedPackResources(packId, collect(registered)),
                info, PackType.CLIENT_RESOURCES, Pack.Position.TOP, true, PackSource.BUILT_IN);
        //?} elif <1.20.5 && neoforge {
        /*Pack.Info info = new Pack.Info(registered.title(), PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of(), false);
        return Pack.create(id, registered.title(), true, new ListingSupplier(registered), info, Pack.Position.TOP, true, PackSource.BUILT_IN);
        *///?} elif <1.20.5 {
        /*Pack.Info info = new Pack.Info(registered.title(), PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of());
        return Pack.create(id, registered.title(), true, new ListingSupplier(registered), info, Pack.Position.TOP, true, PackSource.BUILT_IN);
        *///?} elif neoforge {
        /*PackLocationInfo location = new PackLocationInfo(id, registered.title(), PackSource.BUILT_IN, Optional.empty());
        Pack.Metadata metadata = new Pack.Metadata(registered.title(), PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of(), false);
        return new Pack(location, new ListingSupplier(registered), metadata, new PackSelectionConfig(true, Pack.Position.TOP, true));
        *///?} else {
        /*PackLocationInfo location = new PackLocationInfo(id, registered.title(), PackSource.BUILT_IN, Optional.empty());
        Pack.Metadata metadata = new Pack.Metadata(registered.title(), PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of());
        return new Pack(location, new ListingSupplier(registered), metadata, new PackSelectionConfig(true, Pack.Position.TOP, true));
        *///?}
    }

    private static PackListing collect(RegisteredPack registered) {
        PackListing listing = new PackListing();
        try {
            registered.contents().collect(listing);
            return listing;
        } catch (IOException e) {
            LOGGER.error("Could not list the files of client pack '{}', so it serves no files until the next reload", registered.id(), e);
            return new PackListing();
        }
    }

    //? if >=1.20.2 {
    /*private record ListingSupplier(RegisteredPack registered) implements Pack.ResourcesSupplier {
        //? if <1.20.5 {
        @Override
        public @NotNull ListedPackResources openPrimary(@NotNull String packId) {
            return new ListedPackResources(packId, collect(registered));
        }

        @Override
        public @NotNull ListedPackResources openFull(@NotNull String packId, @NotNull Pack.Info info) {
            return openPrimary(packId);
        }
        //?} elif <26.3 {
        /^@Override
        public @NotNull ListedPackResources openPrimary(@NotNull PackLocationInfo location) {
            return new ListedPackResources(location, collect(registered));
        }

        @Override
        public @NotNull ListedPackResources openFull(@NotNull PackLocationInfo location, @NotNull Pack.Metadata metadata) {
            return openPrimary(location);
        }
        ^///?} else {
        /^@Override
        public @NotNull PackMetadataResources openMetadata(@NotNull PackLocationInfo location) {
            return new ListedPackResources(location, new PackListing());
        }

        @Override
        public @NotNull Stream<PackResources> openResources(@NotNull PackLocationInfo location, @NotNull Pack.Metadata metadata) {
            return Stream.of(new ListedPackResources(location, collect(registered)));
        }
        ^///?}
    }
    *///?}
}
