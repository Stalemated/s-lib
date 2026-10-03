package com.stalemated.lib.config.permissions;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;

public class ClientConfigPermissions {
    public static final Supplier<Boolean> OP_OR_SP = () -> {
        Minecraft client = Minecraft.getInstance();
        boolean inMultiplayer = client.level != null && !client.isLocalServer();
        boolean isOp = client.player != null && client.player.hasPermissions(2);
        return !inMultiplayer || isOp;
    };
    
    public static final Supplier<Boolean> ANYONE = () -> true;
}
