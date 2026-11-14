package com.stalemated.lib.client;

import com.stalemated.lib.config.internal.ConfigRegistry;

//? if fabric{
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
//?}

//? if forge || neoforge {
/*import net.minecraft.client.Minecraft;
*///?}

//? if forge{
/*import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
*///?}

//? if neoforge{
/*import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
//? if <=1.20.4 {
import net.neoforged.fml.common.Mod.EventBusSubscriber;
//?} else {
/^import net.neoforged.fml.common.EventBusSubscriber;
^///?}
import net.neoforged.api.distmarker.Dist;
import com.stalemated.lib.SLib;
*///?}

/**
 * S-Lib's client entrypoint. It tells the synced configs when the player joins or leaves a world
 * and, on Forge and NeoForge, registers the pending key mappings. The loader or {@code SLib}
 * calls it; mods don't use it. Client only.
 */
//? if neoforge
//@EventBusSubscriber(modid = SLib.MOD_ID, value = Dist.CLIENT)
//? if forge
//@SuppressWarnings("removal")
public final class SLibClient 
//? if fabric 
implements ClientModInitializer
{

    //? if fabric{
    /**
     * Fabric {@link ClientModInitializer} entrypoint. Registers the join and disconnect hooks of
     * the synced configs.
     */
    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> ConfigRegistry.onClientJoin(client.hasSingleplayerServer())));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(ConfigRegistry::onClientDisconnect));
    }
    //?}

    //? if forge {
    /*/^* Listens to {@code RegisterKeyMappingsEvent} on the mod bus and to the client login and
     * logout events on the Forge bus. {@code SLib} calls it.
     ^/
    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(SLibClient::onRegisterKeyMappings);

        MinecraftForge.EVENT_BUS.addListener(SLibClient::onClientLogIn);
        MinecraftForge.EVENT_BUS.addListener(SLibClient::onClientLogOut);
    }
    *///?} elif neoforge {
    /*/^* Listens to {@code RegisterKeyMappingsEvent} on the mod bus. The login and logout events
     * arrive through {@code @EventBusSubscriber}. {@code SLib} calls it.
     *
     * @param modBus the S-Lib mod event bus
     ^/
    public static void registerModBusListeners(IEventBus modBus) {
        modBus.addListener(RegisterKeyMappingsEvent.class, SLibClient::onRegisterKeyMappings);
    }
    *///?}

    //? if forge || neoforge {
    /*private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ClientPlatformHelper.registerPendingKeyMappings(event::register);
    }

    //? if neoforge
    //@SubscribeEvent
    private static void onClientLogIn(ClientPlayerNetworkEvent.LoggingIn event) {
        boolean isSingleplayer = Minecraft.getInstance().hasSingleplayerServer();
        ConfigRegistry.onClientJoin(isSingleplayer);
    }

    //? if neoforge
    //@SubscribeEvent
    private static void onClientLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ConfigRegistry.onClientDisconnect();
    }
    *///?}
}
