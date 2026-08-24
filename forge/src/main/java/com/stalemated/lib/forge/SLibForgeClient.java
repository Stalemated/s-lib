package com.stalemated.lib.forge;

import com.stalemated.lib.forge.helper.ForgePlatformHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@SuppressWarnings("removal")
public final class SLibForgeClient {
    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(SLibForgeClient::onRegisterKeyMappings);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyBinding kb : ForgePlatformHelper.KEYBINDINGS) {
            event.register(kb);
        }
    }
}
