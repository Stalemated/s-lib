package com.stalemated.lib;

import com.stalemated.lib.config.internal.ConfigRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.stalemated.lib.network.internal.WrapperChannel;
//?} elif forge {
/*import com.stalemated.lib.client.SLibClient;
import com.stalemated.lib.network.internal.WrapperChannel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
*///?} elif neoforge {
/*import com.stalemated.lib.client.SLibClient;
import com.stalemated.lib.network.internal.WrapperChannel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
//? if <=1.20.4 {
/^import net.neoforged.fml.common.Mod.EventBusSubscriber;
^///?} else {
import net.neoforged.fml.common.EventBusSubscriber;
//?}
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
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

    //? if forge {
    /*public SLib() {
        init();

        WrapperChannel.registerChannel();
        MinecraftForge.EVENT_BUS.addListener(SLib::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(SLib::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(SLib::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(SLib::onServerStopped);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SLibClient.init();
        }
    }
    *///?} elif neoforge {
    /*public SLib(IEventBus modBus, Dist dist) {
        init();

        WrapperChannel.registerModBusListeners(modBus);
        if (dist == Dist.CLIENT) {
            SLibClient.registerModBusListeners(modBus);
        }
    }
    *///?}

    //? if fabric {
    @Override
    public void onInitialize() {
        init();
        WrapperChannel.registerPayloads();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> ConfigRegistry.onPlayerJoinServer(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ConfigRegistry.onPlayerLeaveServer(handler.player));
        ServerLifecycleEvents.SERVER_STARTED.register(ConfigRegistry::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ConfigRegistry.onServerStopped());
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

    //? if neoforge
    //@SubscribeEvent
    //? if forge || neoforge {
    /*private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ConfigRegistry.onPlayerLeaveServer(serverPlayer);
        }
    }
    *///?}

    //? if neoforge
    //@SubscribeEvent
    //? if forge || neoforge {
    /*private static void onServerStarted(ServerStartedEvent event) {
        ConfigRegistry.onServerStarted(event.getServer());
    }
    *///?}

    //? if neoforge
    //@SubscribeEvent
    //? if forge || neoforge {
    /*private static void onServerStopped(ServerStoppedEvent event) {
        ConfigRegistry.onServerStopped();
    }
    *///?}
}
