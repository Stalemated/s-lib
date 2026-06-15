package com.stalemated.lib.forge;

import net.minecraftforge.fml.common.Mod;

import com.stalemated.lib.SLibClient;

@Mod(SLibClient.MOD_ID)
public final class SLibForgeClient {
    public SLibForgeClient() {
        SLibClient.init();
    }
}
