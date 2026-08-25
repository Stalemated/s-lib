package com.stalemated.lib.neoforge.helper;

import com.stalemated.lib.helper.PlatformHelper;
import net.minecraft.client.option.KeyBinding;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NeoForgePlatformHelper implements PlatformHelper {
    public static final List<KeyBinding> KEYBINDINGS = new ArrayList<>();

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

    @Override
    public void registerKeyBinding(KeyBinding keyBinding) {
        KEYBINDINGS.add(keyBinding);
    }
}
