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
     * Resolves a directory path using the following fallback strategy:
     * <ol>
     *     <li>System property override (e.g., for testing environments).</li>
     *     <li>Platform-specific loader query (e.g., FabricLoader or FMLPaths) wrapped in safe error handling.</li>
     *     <li>Static default fallback path.</li>
     * </ol>
     *
     * @param systemProperty The system property key to check for an override.
     * @param platformSupplier Supplier providing the path from the platform loader.
     * @param defaultPath The fallback path if no override or loader path is available.
     * @return The resolved directory {@link Path}.
     */
    private static Path resolveDirectory(String systemProperty, Supplier<Path> platformSupplier, String defaultPath) {
        String override = System.getProperty(systemProperty);
        if (override != null && !override.isEmpty()) {
            return Path.of(override);
        }
        try {
            Path dir = platformSupplier.get();
            if (dir != null) return dir;
        } catch (Throwable ignored) {}

        return Path.of(defaultPath);
    }

    /**
     * Gets the path to the config directory.
     * @return The absolute path to the config directory (e.g. {@code .minecraft/config/}).
     */
    public static Path getConfigDir() {
        return resolveDirectory("slib.config_dir", () -> {
            //? if fabric {
            FabricLoader loader = FabricLoader.getInstance();
            return loader != null ? loader.getConfigDir() : null;
            //?} elif forge || neoforge {
            /*return FMLPaths.CONFIGDIR.get();
            *///?}
        }, "config");
    }

    /**
     * Gets the path to the game directory.
     * @return The absolute path to the game directory (e.g. {@code .minecraft/}).
     */
    public static Path getGameDir() {
        return resolveDirectory("slib.game_dir", () -> {
            //? if fabric {
            FabricLoader loader = FabricLoader.getInstance();
            return loader != null ? loader.getGameDir() : null;
            //?} elif forge || neoforge {
            /*return FMLPaths.GAMEDIR.get();
            *///?}
        }, ".");
    }

    /**
     * Checks if a mod with the specified ID is currently loaded.
     * @param modId The ID of the mod to check.
     * @return {@code true} if the mod is loaded, {@code false} otherwise.
     */
    public static boolean isModLoaded(String modId) {
        //? if fabric {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            return loader != null && loader.isModLoaded(modId);
        //?} elif forge || neoforge {
        /*try {
            return ModList.get().isLoaded(modId);
        *///?}
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Checks if a mod with the specified ID is loaded at the very beginning of the launch cycle.
     * @param modId The ID of the mod to check.
     * @return {@code true} if the mod is present in the loading list, {@code false} otherwise.
     */
    public static boolean isModLoadedAtLaunch(String modId) {
        //? if fabric {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            return loader != null && loader.isModLoaded(modId);
        //?} elif forge || neoforge {
        /*try {
            return LoadingModList.get().getModFileById(modId) != null;
        *///?}
        } catch (Throwable ignored) {
            return false;
        }
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
        try {
            FabricLoader loader = FabricLoader.getInstance();
            return loader != null && EnvType.SERVER == loader.getEnvironmentType();
        //?} elif neoforge && <1.21.11 || forge {
        /*try {
            return Dist.DEDICATED_SERVER == FMLEnvironment.dist;
        *///?} else {
        /*try {
            return Dist.DEDICATED_SERVER == FMLEnvironment.getDist();
        *///?}
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Gets the MinecraftServer instance for a given ServerPlayer.
     * @param player The player.
     * @return The MinecraftServer, or null if player is null.
     */
    //? if >=1.21.11
    //@SuppressWarnings("resource")
     public static MinecraftServer getServer(ServerPlayer player) {
         if (player == null) return null;
         //? if <1.21.11 {
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
        //? if <1.21.11 {
        return server.isSingleplayerOwner(player.getGameProfile());
        //?} else {
        /*return server.isSingleplayerOwner(player.nameAndId());
        *///?}
    }
}
