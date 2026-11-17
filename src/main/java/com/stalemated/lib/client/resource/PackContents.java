package com.stalemated.lib.client.resource;

import java.io.IOException;

/**
 * Lists the files of a client pack registered with
 * {@link SLibResourcePacks#registerClientPack}. Client only.
 */
@FunctionalInterface
public interface PackContents {
    /**
     * Adds every file the pack serves right now. It runs on every resource reload, including the
     * first one and F3+T, so files added to or deleted from a folder since the last reload are
     * picked up. If it throws an {@link IOException}, the error is logged and the pack serves no
     * files until the next reload.
     *
     * @param files receives the files of the pack
     * @throws IOException if a folder of the mod cannot be read
     */
    void collect(PackFiles files) throws IOException;
}
