package com.stalemated.lib.mixin.internal.server;

import com.stalemated.lib.config.internal.ConfigRegistry;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if <1.21.9 {
import com.mojang.authlib.GameProfile;
//?} else {
/*import net.minecraft.server.players.NameAndId;

import java.util.Optional;
*///?}

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    //? if <1.21.9 {
    @Inject(method = {"op", "deop"}, at = @At("TAIL"))
    private void s_lib$resendConfigs(GameProfile profile, CallbackInfo ci) {
        ConfigRegistry.onOperatorStatusChanged((PlayerList) (Object) this, profile.getId());
    }
    //?} else {
    /*@Inject(method = "op(Lnet/minecraft/server/players/NameAndId;Ljava/util/Optional;Ljava/util/Optional;)V", at = @At("TAIL"))
    private void s_lib$resendConfigsAfterOp(NameAndId player, Optional<?> permissions, Optional<Boolean> bypassesPlayerLimit, CallbackInfo ci) {
        ConfigRegistry.onOperatorStatusChanged((PlayerList) (Object) this, player.id());
    }

    @Inject(method = "deop", at = @At("TAIL"))
    private void s_lib$resendConfigsAfterDeop(NameAndId player, CallbackInfo ci) {
        ConfigRegistry.onOperatorStatusChanged((PlayerList) (Object) this, player.id());
    }
    *///?}
}
