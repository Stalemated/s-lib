package com.stalemated.lib.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import com.stalemated.lib.SLib;

@Mod(SLib.MOD_ID)
public final class SLibForge {
    public SLibForge() {
        SLib.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            SLibForgeClient.init();
        }
    }
}
