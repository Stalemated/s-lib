package com.stalemated.lib.fabric;

import com.stalemated.lib.StaleLibClient;
import net.fabricmc.api.ClientModInitializer;

public final class StaleLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        StaleLibClient.init();
    }
}
