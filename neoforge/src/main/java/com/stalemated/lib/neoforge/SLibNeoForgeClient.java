package com.stalemated.lib.neoforge;

import com.stalemated.lib.SLib;
import com.stalemated.lib.config.registry.ConfigRegistry;
import com.stalemated.lib.neoforge.helper.NeoForgePlatformHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = SLib.MOD_ID, value = Dist.CLIENT)
public final class SLibNeoForgeClient {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyBinding keyBinding : NeoForgePlatformHelper.KEYBINDINGS) {
            event.register(keyBinding);
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
    }
}
