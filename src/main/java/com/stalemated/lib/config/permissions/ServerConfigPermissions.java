package com.stalemated.lib.config.permissions;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21.11
//import net.minecraft.server.permissions.Permissions;

/**
 * Ready-made predicates for {@code SyncedConfigBuilder.permissionCheck(...)}. They run on the
 * server thread. The host of a singleplayer or LAN world never goes through them.
 */
public final class ServerConfigPermissions {

    private ServerConfigPermissions() {}

    //? if <1.21.11 {
    /**
     * Allows only operators (permission level 2 or more) to change {@code OVERRIDE_CLIENT}
     * options. This is the default.
     */
    public static final Predicate<ServerPlayer> OP_ONLY = player -> player.hasPermissions(2);
    //?} else {
    /*/^*
     * Allows only operators (gamemaster permission level) to change {@code OVERRIDE_CLIENT}
     * options. This is the default.
     ^/
    public static final Predicate<ServerPlayer> OP_ONLY = player -> player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    *///?}
    /** Allows any player to change {@code OVERRIDE_CLIENT} options. */
    public static final Predicate<ServerPlayer> ANYONE = player -> true;
}
