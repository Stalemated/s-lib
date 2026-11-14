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
 * Loader info with the same API on Fabric, Forge, and NeoForge.
 */
public final class PlatformHelper {

    private PlatformHelper() {}

    /**
     * Returns the mod loader's config directory, for example {@code .minecraft/config}.
     *
     * @return the path to the config directory
     */
    public static Path getConfigDir() {
        //? if fabric {
        return FabricLoader.getInstance().getConfigDir();
        //?} elif forge || neoforge {
        /*return FMLPaths.CONFIGDIR.get();
        *///?}
    }

    /**
     * Returns the game root directory, for example {@code .minecraft}.
     *
     * @return the path to the game directory
     */
    public static Path getGameDir() {
        //? if fabric {
        return FabricLoader.getInstance().getGameDir();
        //?} elif forge || neoforge {
        /*return FMLPaths.GAMEDIR.get();
        *///?}
    }

    /**
     * Returns whether {@code modId} is currently loaded.
     * <p>
     * Do not call during early mixin loading on Forge or NeoForge before {@code ModList} is
     * constructed; use {@link #isModLoadedAtLaunch(String)} instead.
     *
     * @param modId the mod id to check
     * @return {@code true} if the mod is loaded
     * @throws IllegalStateException on Forge or NeoForge if called before {@code ModList} is
     *                               initialized
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
     * Returns whether {@code modId} is present in the early launch mod list, safe to call
     * from mixin plugins before {@code ModList} is constructed.
     *
     * @param modId the mod id to check
     * @return {@code true} if the mod is present at launch
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
     * Returns whether the current JVM is running on a dedicated server.
     *
     * @return {@code true} on a dedicated server, {@code false} on a client, also when it runs
     *         an integrated server
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
     * Returns the {@link MinecraftServer} associated with {@code player}.
     *
     * @param player the server player, or {@code null}
     * @return the player's server instance, or {@code null} if {@code player} is {@code null}
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
     * Returns whether {@code player} is the host of the integrated server (singleplayer or LAN).
     *
     * @param player the player to check, or {@code null}
     * @return {@code true} if the player is the host; {@code false} if {@code player} is
     *         {@code null}
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
