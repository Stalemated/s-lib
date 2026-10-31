package com.stalemated.lib.helper;

import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
//? if <26.1 {
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
*///?}
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
     * Resolves a directory path from a system property override (e.g., for testing environments),
     * or from the platform loader (FabricLoader or FMLPaths) when no override is set.
     *
     * @param systemProperty The system property key to check for an override.
     * @param platformSupplier Supplier providing the path from the platform loader.
     * @return The resolved directory {@link Path}.
     */
    private static Path resolveDirectory(String systemProperty, Supplier<Path> platformSupplier) {
        String override = System.getProperty(systemProperty);
        if (override != null && !override.isEmpty()) {
            return Path.of(override);
        }
        return platformSupplier.get();
    }

    /**
     * Gets the path to the config directory.
     * @return The absolute path to the config directory (e.g. {@code .minecraft/config/}).
     */
    public static Path getConfigDir() {
        return resolveDirectory("slib.config_dir", () -> {
            //? if fabric {
            return FabricLoader.getInstance().getConfigDir();
            //?} elif forge || neoforge {
            /*return FMLPaths.CONFIGDIR.get();
            *///?}
        });
    }

    /**
     * Gets the path to the game directory.
     * @return The absolute path to the game directory (e.g. {@code .minecraft/}).
     */
    public static Path getGameDir() {
        return resolveDirectory("slib.game_dir", () -> {
            //? if fabric {
            return FabricLoader.getInstance().getGameDir();
            //?} elif forge || neoforge {
            /*return FMLPaths.GAMEDIR.get();
            *///?}
        });
    }

    /**
     * Checks if a mod with the specified ID is currently loaded.
     * @param modId The ID of the mod to check.
     * @return {@code true} if the mod is loaded, {@code false} otherwise.
     */
    public static boolean isModLoaded(String modId) {
        //? if fabric {
        return FabricLoader.getInstance().isModLoaded(modId);
        //?} elif forge || neoforge {
        /*ModList mods = ModList.get();
        if (mods == null) {
            throw new IllegalStateException("isModLoaded(\"" + modId + "\") was called before the mod list was built; use isModLoadedAtLaunch");
        }
        return mods.isLoaded(modId);
        *///?}
    }

    /**
     * Checks if a mod with the specified ID is loaded at the very beginning of the launch cycle.
     * @param modId The ID of the mod to check.
     * @return {@code true} if the mod is present in the loading list, {@code false} otherwise.
     */
    public static boolean isModLoadedAtLaunch(String modId) {
        //? if fabric {
        return FabricLoader.getInstance().isModLoaded(modId);
        //?} elif forge || neoforge {
        /*return LoadingModList.get().getModFileById(modId) != null;
        *///?}
    }

    /**
     * Registers a keybind to the client platform.
     * @param keyBinding The keybind to register.
     */
    public static void registerKeyBinding(KeyMapping keyBinding) {
        //? if fabric && <26.1 {
        KeyBindingHelper.registerKeyBinding(keyBinding);
        //?} elif fabric && >=26.1 {
        /*KeyMappingHelper.registerKeyMapping(keyBinding);
        *///?} elif forge || neoforge {
        /*KEYBINDINGS.add(keyBinding);
        *///?}
    }

    /**
     * Checks if the current environment is a dedicated server.
     * @return {@code true} if running on a dedicated server, {@code false} otherwise.
     */
    public static boolean isDedicatedServer() {
        //? if fabric {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
        //?} elif neoforge && <1.21.9 || forge {
        /*return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
        *///?} else {
        /*return FMLEnvironment.getDist() == Dist.DEDICATED_SERVER;
        *///?}
    }

    /**
     * Gets the MinecraftServer instance for a given ServerPlayer.
     * @param player The player.
     * @return The MinecraftServer, or null if player is null.
     */
    //? if >=1.21.6
    //@SuppressWarnings("resource")
     public static MinecraftServer getServer(ServerPlayer player) {
         if (player == null) return null;
         //? if <1.21.6 {
         return player.server;
         //?} else {
         /*return player.level().getServer();
         *///?}
     }

    /**
     * Checks if the specified server player is the host of a singleplayer session.
     * @param player The player to check.
     * @return {@code true} if the player is the singleplayer host, {@code false} otherwise.
     */
    public static boolean isSingleplayerOwner(ServerPlayer player) {
        MinecraftServer server = getServer(player);
        if (server == null) return false;
        //? if <1.21.9 {
        return server.isSingleplayerOwner(player.getGameProfile());
        //?} else {
        /*return server.isSingleplayerOwner(player.nameAndId());
        *///?}
    }
}
