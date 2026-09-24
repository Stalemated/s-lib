package com.stalemated.lib.forge;

import com.stalemated.lib.SLib;
import com.stalemated.lib.config.registry.ConfigRegistry;
import com.stalemated.lib.forge.client.SLibForgeClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(SLib.MOD_ID)
public final class SLibForge {
    public SLibForge() {
        SLib.init();

        MinecraftForge.EVENT_BUS.addListener(SLibForge::onPlayerLoggedIn);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            SLibForgeClient.init();
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity serverPlayer) {
            ConfigRegistry.onPlayerJoinServer(serverPlayer);
        }
    }
}
