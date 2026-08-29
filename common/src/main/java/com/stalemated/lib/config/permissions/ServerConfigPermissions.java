package com.stalemated.lib.config.permissions;

import net.minecraft.server.network.ServerPlayerEntity;
import java.util.function.Predicate;

public class ServerConfigPermissions {
    public static final Predicate<ServerPlayerEntity> OP_ONLY = player -> player.hasPermissionLevel(2);
    public static final Predicate<ServerPlayerEntity> ANYONE = player -> true;
}
