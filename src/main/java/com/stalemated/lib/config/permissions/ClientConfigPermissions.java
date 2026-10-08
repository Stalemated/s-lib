package com.stalemated.lib.config.permissions;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
//? if >=1.21.11
//import net.minecraft.server.permissions.Permissions;

public class ClientConfigPermissions {
    public static final Supplier<Boolean> OP_OR_SP = () -> {
        Minecraft client = Minecraft.getInstance();
        boolean inMultiplayer = client.level != null && !client.isLocalServer();
        //? if <1.21.11 {
        boolean isOp = client.player != null && client.player.hasPermissions(2);
        //?} else {
        /*boolean isOp = client.player != null && client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        *///?}
        return !inMultiplayer || isOp;
    };
    
    public static final Supplier<Boolean> ANYONE = () -> true;
}
