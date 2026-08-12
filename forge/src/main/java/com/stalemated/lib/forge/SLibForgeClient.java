package com.stalemated.lib.forge;

import net.minecraft.client.option.KeyBinding;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.stalemated.lib.SLibClient;

@Mod(SLibClient.MOD_ID)
@SuppressWarnings("removal")
public final class SLibForgeClient {
    public SLibForgeClient() {
        SLibClient.init();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegisterKeyMappings);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyBinding kb : ForgePlatformHelper.KEYBINDINGS) {
            event.register(kb);
        }
    }
}
