package com.stalemated.lib.mixin.internal.client;

import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;

//? if >=26.2 {
/*import com.stalemated.lib.config.internal.ConfigRegistry;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {

    //? if >=26.3 {
    /*@Inject(method = {"setWorldAllowCommands", "setGuestCommandAccess"}, at = @At("TAIL"))
    *///?} elif >=26.2 {
    /*@Inject(method = {"setWorldAllowCommands", "setCommandsAllowedForOtherPlayers"}, at = @At("TAIL"))
    *///?}
    //? if >=26.2 {
    /*private void s_lib$resendConfigs(boolean allowCommands, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        server.execute(() -> ConfigRegistry.onPlayersPermissionChanged(server.getPlayerList()));
    }
    *///?}
}
