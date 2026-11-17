package com.stalemated.lib.client.internal.resource;

import com.stalemated.lib.client.resource.PackContents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ClientPackRegistry {

    public record RegisteredPack(ResourceLocation id, Component title, PackContents contents) {}

    private static final Map<ResourceLocation, RegisteredPack> PACKS = new LinkedHashMap<>();

    private ClientPackRegistry() {}

    public static synchronized void register(ResourceLocation id, Component title, PackContents contents) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(contents, "contents cannot be null");
        if (PACKS.containsKey(id)) {
            throw new IllegalArgumentException("Client pack '" + id + "' is already registered");
        }
        PACKS.put(id, new RegisteredPack(id, title, contents));
    }

    public static synchronized List<RegisteredPack> all() {
        return List.copyOf(PACKS.values());
    }

    public static synchronized void clear() {
        PACKS.clear();
    }
}
