package com.stalemated.lib.forge;

import com.stalemated.lib.helper.PlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;

import java.nio.file.Path;

public class ForgePlatformHelper implements PlatformHelper {
    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isModLoadedAtLaunch(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }
}
