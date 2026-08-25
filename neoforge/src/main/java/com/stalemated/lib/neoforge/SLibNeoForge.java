package com.stalemated.lib.neoforge;

import com.stalemated.lib.SLib;
import net.neoforged.fml.common.Mod;

@Mod(SLib.MOD_ID)
public final class SLibNeoForge {
    public SLibNeoForge() {
        SLib.init();
    }
}
