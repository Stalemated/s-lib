package com.stalemated.lib.config.network;

/**
 * How an option is synced between the server and its clients. Set it with {@code @Sync} and read
 * it with {@code OptionInfo.getSyncMode()}. The default is {@link #OVERRIDE_CLIENT}.
 */
public enum SyncMode {
    /**
     * The option is local to the machine running the code, client or server. It is never sent
     * over the network, and the server never overrides it. Use it for visual and UI preferences
     * of the client.
     */
    NONE,

    /**
     * The server decides. On a server with the mod, its value replaces the local value of the
     * client in memory, not in the client's file; on a server without the mod, the option uses its
     * default. Changing it from a client needs the {@code permissionCheck} of the config. Use it
     * for gameplay settings.
     */
    OVERRIDE_CLIENT,

    /**
     * The client sends its value to the server, but the server does not override the client. It
     * is sent when the client joins a server with the mod or opens a singleplayer world, and
     * after each change; the server keeps it per player. Use it for client preferences the server
     * needs to know.
     */
    INFORM_SERVER;

    /**
     * Returns whether this mode uses the network.
     *
     * @return {@code true} for {@link #OVERRIDE_CLIENT} and {@link #INFORM_SERVER}
     */
    public boolean isSynced() {
        return this == OVERRIDE_CLIENT || this == INFORM_SERVER;
    }
}
