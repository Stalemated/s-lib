package com.stalemated.lib.helper;

import net.minecraft.client.option.KeyBinding;
import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * An abstraction layer for retrieving platform-specific information and performing actions across different modloaders.
 * <p>
 * This allows common code to perform tasks like checking loaded mods or getting
 * directory paths seamlessly across multiple modloaders.
 */
public interface PlatformHelper {

    /**
     * The loaded instance of the PlatformHelper.
     * Resolved dynamically via {@link ServiceLoader} at runtime based on the active modloader.
     */
    PlatformHelper INSTANCE = ServiceLoader.load(PlatformHelper.class)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("PlatformHelper not found!"));

    /**
     * Gets the path to the configuration directory.
     * @return The absolute path to the configuration directory (e.g. {@code .minecraft/config/}).
     */
    Path getConfigDir();

    /**
     * Gets the path to the game directory.
     * @return The absolute path to the game directory (e.g. {@code .minecraft/}).
     */
    Path getGameDir();

    /**
     * Checks if a mod with the specified ID is currently loaded.
     * This method is safe to call during normal runtime.
     *
     * @param modId The ID of the mod to check.
     * @return {@code true} if the mod is loaded, {@code false} otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Checks if a mod with the specified ID is loaded at the very beginning of the launch cycle.
     * This is particularly useful in environments like Mixin config plugins where the regular
     * mod list might not be fully populated or initialized yet.
     *
     * @param modId The ID of the mod to check.
     * @return {@code true} if the mod is present in the loading list, {@code false} otherwise.
     */
    boolean isModLoadedAtLaunch(String modId);

    /**
     * Registers a keybind to the client platform.
     * On Fabric, this registers directly to {@code KeyBindingHelper}.
     * On Forge, this queues the keybinding to be registered during the {@code RegisterKeyMappingsEvent}.
     *
     * @param keyBinding The keybind to register.
     */
    void registerKeyBinding(KeyBinding keyBinding);
}
