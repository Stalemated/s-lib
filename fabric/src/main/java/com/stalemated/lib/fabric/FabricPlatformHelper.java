package com.stalemated.lib.fabric;

import com.stalemated.lib.helper.PlatformHelper;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;

import java.nio.file.Path;

public class FabricPlatformHelper implements PlatformHelper {
    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isModLoadedAtLaunch(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public void registerKeyBinding(KeyBinding keyBinding) {
        KeyBindingHelper.registerKeyBinding(keyBinding);
    }
}
