package com.stalemated.lib.neoforge;

import com.stalemated.lib.SLib;
import com.stalemated.lib.config.registry.ConfigRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(SLib.MOD_ID)
@EventBusSubscriber(modid = SLib.MOD_ID)
public final class SLibNeoForge {
    public SLibNeoForge() {
        SLib.init();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity serverPlayer) {
            ConfigRegistry.onPlayerJoinServer(serverPlayer);
        }
    }
}
