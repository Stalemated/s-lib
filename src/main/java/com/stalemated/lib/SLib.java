package com.stalemated.lib;

import com.stalemated.lib.config.registry.ConfigRegistry;
import com.stalemated.lib.network.NetworkHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if fabric
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

//? if forge
/*import net.minecraftforge.common.MinecraftForge;
*//*import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;*/

//? if neoforge
/*import net.neoforged.bus.api.SubscribeEvent;
*//*import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;*/

//? if forge || neoforge
/*@Mod(MOD_ID)*/
//? if neoforge
/*@EventBusSubscriber(modid = MOD_ID)*/
public final class SLib 
//? if fabric 
implements ModInitializer 
{
    public static final String MOD_ID = "s_lib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    //? if forge || neoforge
    /*public SLib() {
*/        /*init();
        
        //? if forge {
        ^MinecraftForge.EVENT_BUS.addListener(SLib::onPlayerLoggedIn);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SLibClient.init();
        }
        //?}
    }*/

    //? if fabric
    @Override
    public void onInitialize() {
        init();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> ConfigRegistry.onPlayerJoinServer(handler.player)));
    }

    public static void init() {
        NetworkHelper.init();
        LOGGER.info("S-Lib loaded successfully");
    }

    //? if forge
    /*private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
*/        /*if (event.getEntity() instanceof ServerPlayerEntity serverPlayer) {
            ConfigRegistry.onPlayerJoinServer(serverPlayer);
        }
    }*/

    //? if neoforge
    /*@SubscribeEvent
*/    /*public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity serverPlayer) {
            ConfigRegistry.onPlayerJoinServer(serverPlayer);
        }
    }*/
}
