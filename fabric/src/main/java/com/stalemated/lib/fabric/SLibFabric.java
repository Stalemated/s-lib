package com.stalemated.lib.fabric;

import com.stalemated.lib.SLib;
import net.fabricmc.api.ModInitializer;

public final class SLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SLib.init();
    }
}
