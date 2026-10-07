package com.stalemated.lib.client;

import com.stalemated.lib.config.registry.ConfigRegistry;

//? if fabric{
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
//?}

//? if forge || neoforge {
/*import com.stalemated.lib.helper.PlatformHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import com.stalemated.lib.SLib;

@EventBusSubscriber(modid = SLib.MOD_ID, value = Dist.CLIENT)
*///?}
//? if forge
//@SuppressWarnings("removal")
public final class SLibClient 
//? if fabric 
implements ClientModInitializer
{

    //? if fabric{
    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> ConfigRegistry.onClientJoin(client.hasSingleplayerServer())));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(ConfigRegistry::onClientDisconnect));
    }
    //?}

    //? if forge {
    /*public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(SLibClient::onRegisterKeyMappings);

        MinecraftForge.EVENT_BUS.addListener(SLibClient::onClientLogIn);
        MinecraftForge.EVENT_BUS.addListener(SLibClient::onClientLogOut);
    }
    *///?}

    //? if forge || neoforge {
    /*//? if neoforge
    //@SubscribeEvent
    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping kb : PlatformHelper.KEYBINDINGS) {
            event.register(kb);
        }
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
