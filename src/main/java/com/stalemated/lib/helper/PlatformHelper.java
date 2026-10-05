package com.stalemated.lib.helper;

import java.nio.file.Path;
import net.minecraft.client.KeyMapping;

//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//?}

//? if forge || neoforge {
/*import java.util.ArrayList;
import java.util.List;
*///?}

//? if forge {
/*import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
*///?}

//? if neoforge {
/*import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
*///?}

/**
 * An abstraction layer for retrieving platform-specific information and performing actions across different modloaders.
 */
public class PlatformHelper {

    //? if forge || neoforge
    //public static final List<KeyMapping> KEYBINDINGS = new ArrayList<>();

    /**
     * Gets the path to the config directory.
     * @return The absolute path to the config directory (e.g. {@code .minecraft/config/}).
     */
    public static Path getConfigDir() {
        //? if fabric
        return FabricLoader.getInstance().getConfigDir();
        //? if forge || neoforge
        //return FMLPaths.CONFIGDIR.get();
    }

    /**
     * Gets the path to the game directory.
     * @return The absolute path to the game directory (e.g. {@code .minecraft/}).
     */
    public static Path getGameDir() {
        //? if fabric
        return FabricLoader.getInstance().getGameDir();
        //? if forge|| neoforge
        //return FMLPaths.GAMEDIR.get();
    }

    /**
     * Checks if a mod with the specified ID is currently loaded.
     * @param modId The ID of the mod to check.
     * @return {@code true} if the mod is loaded, {@code false} otherwise.
     */
    public static boolean isModLoaded(String modId) {
        //? if fabric
        return FabricLoader.getInstance().isModLoaded(modId);
        //? if forge|| neoforge
        //return ModList.get().isLoaded(modId);
    }

    /**
     * Checks if a mod with the specified ID is loaded at the very beginning of the launch cycle.
     * @param modId The ID of the mod to check.
     * @return {@code true} if the mod is present in the loading list, {@code false} otherwise.
     */
    public static boolean isModLoadedAtLaunch(String modId) {
        //? if fabric
        return FabricLoader.getInstance().isModLoaded(modId);
        //? if forge|| neoforge
        //return LoadingModList.get().getModFileById(modId) != null;
    }

    /**
     * Registers a keybind to the client platform.
     * @param keyBinding The keybind to register.
     */
    public static void registerKeyBinding(KeyMapping keyBinding) {
        //? if fabric {
        KeyBindingHelper.registerKeyBinding(keyBinding);
        //?}
        //? if forge || neoforge {
        /*KEYBINDINGS.add(keyBinding);
        *///?}
    }

    /**
     * Checks if the current environment is a dedicated server.
     * @return {@code true} if running on a dedicated server, {@code false} otherwise.
     */
    public static boolean isDedicatedServer() {
        //? if fabric
        return EnvType.SERVER == FabricLoader.getInstance().getEnvironmentType();
        //? if forge|| neoforge
        //return Dist.DEDICATED_SERVER == FMLEnvironment.dist;
    }
}
