package com.stalemated.lib.forge;

import net.minecraftforge.fml.common.Mod;

import com.stalemated.lib.StaleLibClient;

@Mod(StaleLibClient.MOD_ID)
public final class StaleLibForgeClient {
    public StaleLibForgeClient() {
        StaleLibClient.init();
    }
}
