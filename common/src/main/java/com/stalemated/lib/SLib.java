package com.stalemated.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SLib {
    public static final String MOD_ID = "s_lib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("S-Lib loaded successfully");
    }
}
