package com.stalemated.lib.fabric;

import com.stalemated.lib.SLib;
import com.stalemated.lib.config.registry.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class SLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SLib.init();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> ConfigRegistry.onPlayerJoinServer(handler.player)));
    }
}
