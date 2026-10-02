package com.stalemated.lib.client;

import com.stalemated.lib.config.registry.ConfigRegistry;

//? if fabric
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

//? if forge
/*import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
*//*import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;*/

//? if neoforge
/*import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
*//*import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;*/

//? if neoforge
/*@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)*/
public final class SLibClient 
//? if fabric 
implements ClientModInitializer 
{

    //? if fabric
    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> ConfigRegistry.onClientJoin(client.isIntegratedServerRunning())));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(ConfigRegistry::onClientDisconnect));
    }

    //? if forge
    /*public static void init() {
*/        /*FMLJavaModLoadingContext.get().getModEventBus().addListener(SLibClient::onRegisterKeyMappings);
        MinecraftForge.EVENT_BUS.addListener(SLibClient::onClientLogIn);
        MinecraftForge.EVENT_BUS.addListener(SLibClient::onClientLogOut);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyBinding kb : PlatformHelper.KEYBINDINGS) {
            event.register(kb);
        }
    }

    private static void onClientLogIn(ClientPlayerNetworkEvent.LoggingIn event) {
        boolean isSingleplayer = MinecraftClient.getInstance().isInSingleplayer();
        ConfigRegistry.onClientJoin(isSingleplayer);
    }

    private static void onClientLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ConfigRegistry.onClientDisconnect();
    }*/

    //? if neoforge
    /*@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
*/    /*public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            for (KeyBinding keyBinding : PlatformHelper.KEYBINDINGS) {
                event.register(keyBinding);
            }
        }
    }

    @SubscribeEvent
    public static void onClientLogIn(ClientPlayerNetworkEvent.LoggingIn event) {
        boolean isSingleplayer = MinecraftClient.getInstance().isInSingleplayer();
        ConfigRegistry.onClientJoin(isSingleplayer);
    }

    @SubscribeEvent
    public static void onClientLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ConfigRegistry.onClientDisconnect();
    }*/
}
