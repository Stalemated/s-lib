package com.stalemated.lib;

import com.stalemated.lib.config.registry.ConfigRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
//? if >=1.20.5
//import com.stalemated.lib.network.NetworkHelper;
//?} elif forge {
/*import com.stalemated.lib.client.SLibClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
*///?} elif neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
//? if <=1.20.4 {
import net.neoforged.fml.common.Mod.EventBusSubscriber;
//?} else {
/^import net.neoforged.fml.common.EventBusSubscriber;
^///?}
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
*///?}

//? if forge || neoforge {
/*import net.minecraft.server.level.ServerPlayer;

@Mod(SLib.MOD_ID)
*///?}
//? if neoforge
//@EventBusSubscriber(modid = SLib.MOD_ID)
public final class SLib 
//? if fabric 
implements ModInitializer 
{
    public static final String MOD_ID = "s_lib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("S-Lib loaded successfully");
    }

    //? if forge || neoforge {
    /*public SLib() {
        init();

        //? if forge {
        /^MinecraftForge.EVENT_BUS.addListener(SLib::onPlayerLoggedIn);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SLibClient.init();
        }
        ^///?}
    }
    *///?}

    //? if fabric {
    @Override
    public void onInitialize() {
        init();
        //? if >=1.20.5
        //NetworkHelper.registerPayloads();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> ConfigRegistry.onPlayerJoinServer(handler.player)));
    }
    //?}

    //? if neoforge
    //@SubscribeEvent
    //? if forge || neoforge {
    /*private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ConfigRegistry.onPlayerJoinServer(serverPlayer);
        }
    }
    *///?}
}
