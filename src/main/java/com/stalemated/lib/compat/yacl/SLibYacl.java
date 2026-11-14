package com.stalemated.lib.compat.yacl;

import com.stalemated.lib.helper.PlatformHelper;


/**
 * Utility for checking whether Yet Another Config Lib (YACL) is loaded at runtime.
 */
public final class SLibYacl {

    private static final String YACL_MOD_ID = "yet_another_config_lib_v3";

    private SLibYacl() {}

    /**
     * Returns whether YACL v3 ({@code yet_another_config_lib_v3}) is currently loaded.
     *
     * @return {@code true} if YACL is loaded
     * @throws IllegalStateException on Forge and NeoForge, if called before the mod list is built
     */
    public static boolean isAvailable() {
        return PlatformHelper.isModLoaded(YACL_MOD_ID);
    }
}
