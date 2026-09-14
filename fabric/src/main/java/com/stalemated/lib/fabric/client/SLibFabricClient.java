package com.stalemated.lib.fabric.client;

import com.stalemated.lib.config.registry.ConfigRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class SLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> ConfigRegistry.onClientJoin(client.isIntegratedServerRunning())));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(ConfigRegistry::onClientDisconnect));
    }
}
