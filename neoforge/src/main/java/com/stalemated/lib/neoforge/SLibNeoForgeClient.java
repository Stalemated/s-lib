package com.stalemated.lib.neoforge;

import com.stalemated.lib.SLibClient;
import net.minecraft.client.option.KeyBinding;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(SLibClient.MOD_ID)
public final class SLibNeoForgeClient {
    public SLibNeoForgeClient(IEventBus modEventBus) {
        SLibClient.init();
        modEventBus.addListener(this::onRegisterKeyMappings);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyBinding kb : NeoForgePlatformHelper.KEYBINDINGS) {
            event.register(kb);
        }
    }
}
