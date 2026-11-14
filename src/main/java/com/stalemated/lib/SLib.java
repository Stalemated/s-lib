package com.stalemated.lib;

import com.stalemated.lib.config.internal.ConfigRegistry;
import com.stalemated.lib.predicate.target.internal.TagReloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.stalemated.lib.network.internal.WrapperChannel;
//?} elif forge {
/*import com.stalemated.lib.client.SLibClient;
import com.stalemated.lib.network.internal.WrapperChannel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
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
import net.neoforged.fml.common.Mod.EventBusSubscriber;
//?} else {
/^import net.neoforged.fml.common.EventBusSubscriber;
^///?}
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
*///?}
//? if forge || neoforge
//import net.minecraft.server.level.ServerPlayer;

/**
 * S-Lib entrypoint, created by the mod loader. Mods only need {@link #MOD_ID} and
 * {@link #LOGGER} from this class.
 */
//? if forge || neoforge
//@Mod(SLib.MOD_ID)
//? if neoforge
//@EventBusSubscriber(modid = SLib.MOD_ID)
public final class SLib 
//? if fabric 
implements ModInitializer 
{
    /** S-Lib's mod id. */
    public static final String MOD_ID = "s_lib";
    /** Shared SLF4J logger for S-Lib. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static void init() {
        LOGGER.info("S-Lib loaded successfully");
    }

    //? if forge {
    /*/^*
     * Called by Forge. Registers the network channel and the game event listeners, and on a
     * client calls {@link SLibClient#init()}.
     ^/
    public SLib() {
        init();

        WrapperChannel.registerChannel();
        MinecraftForge.EVENT_BUS.addListener(SLib::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(SLib::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(SLib::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(SLib::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(SLib::onTagsUpdated);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SLibClient.init();
        }
    }
    *///?} elif neoforge {
    /*/^*
     * Called by NeoForge. Registers the network payload on the mod bus and, on a client, the
     * client listeners. The game events reach this class through {@code @EventBusSubscriber}.
     *
     * @param modBus the S-Lib mod event bus
     * @param dist the physical side
     ^/
    public SLib(IEventBus modBus, Dist dist) {
        init();

        WrapperChannel.registerModBusListeners(modBus);
        if (dist == Dist.CLIENT) {
            SLibClient.registerModBusListeners(modBus);
        }
    }
    *///?}

    //? if fabric {
    /**
     * Called by Fabric Loader. Registers the network channel and the game event listeners.
     */
    @Override
    public void onInitialize() {
        init();
        WrapperChannel.registerPayloads();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> ConfigRegistry.onPlayerJoinServer(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ConfigRegistry.onPlayerLeaveServer(handler.player));
        ServerLifecycleEvents.SERVER_STARTED.register(ConfigRegistry::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ConfigRegistry.onServerStopped());
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> TagReloads.onTagsReloaded());
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

    //? if neoforge
    //@SubscribeEvent
    //? if forge || neoforge {
    /*private static void onTagsUpdated(TagsUpdatedEvent event) {
        TagReloads.onTagsReloaded();
    }
    *///?}
}
