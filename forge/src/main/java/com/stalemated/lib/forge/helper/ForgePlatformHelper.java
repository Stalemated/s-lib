package com.stalemated.lib.forge.helper;

import com.stalemated.lib.helper.PlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraft.client.option.KeyBinding;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ForgePlatformHelper implements PlatformHelper {
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
