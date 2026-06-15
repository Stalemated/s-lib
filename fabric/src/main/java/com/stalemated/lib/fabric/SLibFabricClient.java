package com.stalemated.lib.fabric;

import com.stalemated.lib.SLibClient;
import net.fabricmc.api.ClientModInitializer;

public final class SLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SLibClient.init();
    }
}
