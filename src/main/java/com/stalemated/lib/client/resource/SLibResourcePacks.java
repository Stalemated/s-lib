package com.stalemated.lib.client.resource;

import com.stalemated.lib.client.internal.resource.ClientPackRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Client resource packs that are always on, with files the mod lists in code. Client only.
 */
public final class SLibResourcePacks {

    private SLibResourcePacks() {}

    /**
     * Adds a client resource pack that is always on: the player cannot turn it off or move it,
     * and it sits above every other pack. Its metadata is made in code with the pack format of
     * the running game, so it needs no {@code pack.mcmeta}. {@code contents} lists the files
     * again on every resource reload, so F3+T sees files added or deleted since the last one.
     * <p>
     * Call it from the client initializer of the mod, before the first resource reload. A pack
     * registered later shows up on the next reload.
     *
     * @param id id of the pack, for example {@code mymod:resources}
     * @param title name shown on the resource pack screen
     * @param contents lists the files of the pack
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a pack with this {@code id} is already registered
     */
    public static void registerClientPack(ResourceLocation id, Component title, PackContents contents) {
        ClientPackRegistry.register(id, title, contents);
    }
}
