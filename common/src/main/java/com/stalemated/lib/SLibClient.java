package com.stalemated.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SLibClient {
    public static final String MOD_ID = "stalelib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Stale Lib loaded successfully");
    }
}
