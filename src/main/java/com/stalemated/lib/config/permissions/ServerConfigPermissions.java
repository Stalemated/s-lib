package com.stalemated.lib.config.permissions;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;

public class ServerConfigPermissions {
    public static final Predicate<ServerPlayer> OP_ONLY = player -> player.hasPermissions(2);
    public static final Predicate<ServerPlayer> ANYONE = player -> true;
}
