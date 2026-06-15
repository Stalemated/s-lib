package com.stalemated.lib.helper;

import java.nio.file.Path;
import java.util.ServiceLoader;

public interface PlatformHelper {

    PlatformHelper INSTANCE = ServiceLoader.load(PlatformHelper.class)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("PlatformHelper not found!"));

    Path getConfigDir();

    Path getGameDir();

    boolean isModLoaded(String modId);
}
