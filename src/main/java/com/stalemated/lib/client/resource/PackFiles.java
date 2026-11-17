package com.stalemated.lib.client.resource;

import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The files of a client pack for one resource reload, filled by {@link PackContents}. Each
 * location is a full resource path, such as {@code mymod:textures/gui/stone.png} or
 * {@code mymod:font/pixel.json}. Client only.
 */
public interface PackFiles {
    /**
     * Serves {@code file} at {@code location}. The file is read when the game asks for it, not
     * copied.
     *
     * @param location the resource location of the file
     * @param file the file on disk
     * @throws NullPointerException if {@code location} or {@code file} is {@code null}
     * @throws IllegalArgumentException if {@code location} was already listed in this reload
     */
    void file(ResourceLocation location, Path file);

    /**
     * Serves {@code content} at {@code location}, for files the mod generates, such as a font
     * definition. The array is copied.
     *
     * @param location the resource location of the file
     * @param content the bytes of the file
     * @throws NullPointerException if {@code location} or {@code content} is {@code null}
     * @throws IllegalArgumentException if {@code location} was already listed in this reload
     */
    void bytes(ResourceLocation location, byte[] content);

    /**
     * Serves every file under {@code root/assets/<namespace>/} as {@code <namespace>:<path>}, as
     * a resource pack folder would. Files whose path is not a valid resource location are logged
     * and skipped. A missing {@code assets} folder adds nothing.
     *
     * @param root the folder that holds {@code assets}
     * @throws NullPointerException if {@code root} is {@code null}
     * @throws IllegalArgumentException if a file was already listed in this reload
     * @throws IOException if the folder cannot be read
     */
    void assetsFolder(Path root) throws IOException;
}
