package com.stalemated.lib.helper;

import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
//?}

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
//? if <1.21.10 {
import net.neoforged.fml.loading.LoadingModList;
//?} else {
/^import net.neoforged.fml.loading.FMLLoader;
^///?}
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
*///?}

/**
 * An abstraction layer for retrieving platform-specific information and performing actions across different modloaders.
 */
public final class PlatformHelper {

    private PlatformHelper() {}

    /**
     * Gets the path to the config directory.
     * @return The absolute path to the config directory (e.g. {@code .minecraft/config/}).
     */
    public static Path getConfigDir() {
        //? if fabric {
        return FabricLoader.getInstance().getConfigDir();
        //?} elif forge || neoforge {
        /*return FMLPaths.CONFIGDIR.get();
        *///?}
    }

    /**
     * Gets the path to the game directory.
     * @return The absolute path to the game directory (e.g. {@code .minecraft/}).
     */
    public static Path getGameDir() {
        //? if fabric {
        return FabricLoader.getInstance().getGameDir();
        //?} elif forge || neoforge {
        /*return FMLPaths.GAMEDIR.get();
        *///?}
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
        //?} elif forge || neoforge && <1.21.10 {
        /*return LoadingModList.get().getModFileById(modId) != null;
        *///?} elif neoforge {
        /*return FMLLoader.getCurrent().getLoadingModList().getModFileById(modId) != null;
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
