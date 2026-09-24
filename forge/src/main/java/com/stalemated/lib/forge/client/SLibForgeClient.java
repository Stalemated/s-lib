package com.stalemated.lib.forge.client;

import com.stalemated.lib.config.registry.ConfigRegistry;
import com.stalemated.lib.forge.helper.ForgePlatformHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@SuppressWarnings("removal")
public final class SLibForgeClient {
    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(SLibForgeClient::onRegisterKeyMappings);

        MinecraftForge.EVENT_BUS.addListener(SLibForgeClient::onClientLogIn);
        MinecraftForge.EVENT_BUS.addListener(SLibForgeClient::onClientLogOut);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyBinding kb : ForgePlatformHelper.KEYBINDINGS) {
            event.register(kb);
        }
    }

    private static void onClientLogIn(ClientPlayerNetworkEvent.LoggingIn event) {
        boolean isSingleplayer = MinecraftClient.getInstance().isInSingleplayer();
        ConfigRegistry.onClientJoin(isSingleplayer);
    }

    private static void onClientLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ConfigRegistry.onClientDisconnect();
    }
}
