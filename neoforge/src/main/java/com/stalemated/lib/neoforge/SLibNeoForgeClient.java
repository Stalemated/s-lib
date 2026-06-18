package com.stalemated.lib.neoforge;

import com.stalemated.lib.SLibClient;
import net.neoforged.fml.common.Mod;

@Mod(SLibClient.MOD_ID)
public final class SLibNeoForgeClient {
    public SLibNeoForgeClient() {
        SLibClient.init();
    }
}
