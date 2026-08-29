package com.stalemated.lib.config.permissions;

import net.minecraft.client.MinecraftClient;
import java.util.function.Supplier;

public class ClientConfigPermissions {
    public static final Supplier<Boolean> OP_OR_SP = () -> {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean inMultiplayer = client.world != null && !client.isInSingleplayer();
        boolean isOp = client.player != null && client.player.hasPermissionLevel(2);
        return !inMultiplayer || isOp;
    };
    
    public static final Supplier<Boolean> ANYONE = () -> true;
}
