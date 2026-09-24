package com.stalemated.lib.config.network;

public enum SyncMode {
    /**
     * The option is strictly local to the machine running the code (client or server).
     * It is never sent over the network, and the server will never override it.
     * Intended for client-side visual preferences, keybinds, UI settings, etc.
     */
    NONE,

    /**
     * The server is authoritative. When connected to a server,
     * the server's value overrides the client's local config in memory.
     * Intended for gameplay config and any server-side mechanic.
     */
    OVERRIDE_CLIENT,

    /**
     * The client sends its value to the server, but the server does not override the client.
     * Useful for client capability signaling or cosmetic preferences the server needs to know.
     */
    INFORM_SERVER;

    /**
     * @return a boolean value showing if the config is synced.
     */
    public boolean isSynced() {
        return this == OVERRIDE_CLIENT || this == INFORM_SERVER;
    }
}
