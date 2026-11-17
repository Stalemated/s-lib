package com.stalemated.lib.client.internal.resource;

import com.stalemated.lib.client.resource.PackFiles;
import com.stalemated.lib.util.ResourceIds;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.stalemated.lib.SLib.LOGGER;

public final class PackListing implements PackFiles {

    private final Map<ResourceLocation, IoSupplier<InputStream>> files = new LinkedHashMap<>();

    @Override
    public void file(ResourceLocation location, Path file) {
        Objects.requireNonNull(file, "file cannot be null");
        add(location, IoSupplier.create(file));
    }

    @Override
    public void bytes(ResourceLocation location, byte[] content) {
        byte[] copy = Objects.requireNonNull(content, "content cannot be null").clone();
        add(location, () -> new ByteArrayInputStream(copy));
    }

    @Override
    public void assetsFolder(Path root) throws IOException {
        Path assets = Objects.requireNonNull(root, "root cannot be null").resolve("assets");
        if (!Files.isDirectory(assets)) return;

        try (Stream<Path> walked = Files.walk(assets)) {
            for (Path file : walked.filter(Files::isRegularFile).toList()) {
                Path relative = assets.relativize(file);
                if (relative.getNameCount() < 2) continue;
                String namespace = relative.getName(0).toString();
                String path = relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/');

                try {
                    file(ResourceIds.of(namespace, path), file);
                } catch (ResourceLocationException e) {
                    LOGGER.warn("Skipping '{}' in the assets folder '{}': {}", file, root, e.getMessage());
                }
            }
        }
    }

    private void add(ResourceLocation location, IoSupplier<InputStream> supplier) {
        Objects.requireNonNull(location, "location cannot be null");
        if (files.putIfAbsent(location, supplier) != null) {
            throw new IllegalArgumentException("Pack file '" + location + "' was listed twice");
        }
    }

    @Nullable IoSupplier<InputStream> resource(ResourceLocation location) {
        return files.get(location);
    }

    void list(String namespace, String folder, PackResources.ResourceOutput output) {
        String prefix = folder.endsWith("/") ? folder : folder + "/";
        files.forEach((location, supplier) -> {
            if (location.getNamespace().equals(namespace) && location.getPath().startsWith(prefix)) {
                output.accept(location, supplier);
            }
        });
    }

    Set<String> namespaces() {
        return files.keySet().stream().map(ResourceLocation::getNamespace).collect(Collectors.toUnmodifiableSet());
    }
}
