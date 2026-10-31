package com.stalemated.lib.compat.yacl;

import com.stalemated.lib.helper.PlatformHelper;


/**
 * Utility class for checking the availability of the Yet Another Config Lib (YACL) mod.
 */
public final class SLibYacl {

    private static final String YACL_MOD_ID = "yet_another_config_lib_v3";

    private SLibYacl() {}

    /**
     * Checks if the Yet Another Config Lib (YACL) mod is available.
     *
     * @return true if YACL is available, false otherwise.
     */
    public static boolean isAvailable() {
        return PlatformHelper.isModLoaded(YACL_MOD_ID);
    }
}
