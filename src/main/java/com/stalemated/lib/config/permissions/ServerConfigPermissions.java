package com.stalemated.lib.config.permissions;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21.11
//import net.minecraft.server.permissions.Permissions;

public class ServerConfigPermissions {
    //? if <1.21.11 {
    public static final Predicate<ServerPlayer> OP_ONLY = player -> player.hasPermissions(2);
    //?} else {
    /*public static final Predicate<ServerPlayer> OP_ONLY = player -> player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    *///?}
    public static final Predicate<ServerPlayer> ANYONE = player -> true;
}
