package com.stalemated.lib.config.network;

/**
 * Connection state of a synced config on this side, from
 * {@code SyncedConfigManager.getConnectionState()}. It changes when the client joins a world,
 * when the config of the server arrives and when the client leaves.
 */
public enum ConnectionState {
    /**
     * No world is open on this client, or this is a dedicated server. {@code get()} returns the
     * local config and every option can be changed.
     */
    DISCONNECTED,
    /**
     * This client hosts the world, also when it is open to LAN. It uses the local config, which
     * it shares with the integrated server, and every option can be changed.
     */
    SINGLEPLAYER,
    /**
     * On a server that has not sent this config: no S-Lib, or not this mod. {@code OVERRIDE_CLIENT}
     * options use their defaults and cannot be changed. This is also the state right after joining,
     * until the config of the server arrives.
     */
    MULTIPLAYER_UNMODDED,
    /**
     * The server sent this config. {@code get()} returns the server's copy, and the permission to
     * change {@code OVERRIDE_CLIENT} options comes from the server.
     */
    MULTIPLAYER_MODDED
}
