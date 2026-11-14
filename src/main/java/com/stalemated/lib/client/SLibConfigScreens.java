package com.stalemated.lib.client;

import com.stalemated.lib.client.internal.ConfigScreenFactories;
import com.stalemated.lib.compat.yacl.SLibYacl;
import com.stalemated.lib.helper.PlatformHelper;
import net.minecraft.client.gui.screens.Screen;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.stalemated.lib.SLib.LOGGER;

//? if forge {
/*import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
*///?}

//? if neoforge {
/*import net.neoforged.fml.ModList;
//? if <=1.20.4 {
import net.neoforged.neoforge.client.ConfigScreenHandler;
//?} else {
/^import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
^///?}
*///?}

/**
 * Registers YACL config screens, only when YACL is installed. Client only.
 * <p>
 * On Fabric, Mod Menu shows them through S-Lib's integration. On Forge and NeoForge, they are
 * registered as the config screen extension point of the mod container.
 */
public final class SLibConfigScreens {

    private SLibConfigScreens() {}

    /**
     * Registers the config screen of {@code modId}. It checks {@code modId} first. If YACL is not
     * installed, it logs a debug message and returns without calling {@code factory}.
     * <p>
     * With Mod Menu 15.0.2 or older (1.20.1 to 1.21.8), a {@code modmenu} entrypoint of the mod
     * itself hides this screen.
     *
     * @param modId id of a loaded mod
     * @param factory supplies the function that makes the screen from its parent; it is only called when YACL is installed, so the screen class never loads without YACL
     * @throws NullPointerException if {@code modId}, {@code factory} or the function it supplies is {@code null}
     * @throws IllegalArgumentException if no mod with {@code modId} is loaded
     * @throws IllegalStateException if {@code modId} already has a config screen, or, on Forge and NeoForge, if called before the mod list is built
     */
    public static void register(String modId, Supplier<Function<Screen, Screen>> factory) {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(factory, "factory cannot be null");
        if (!PlatformHelper.isModLoaded(modId)) {
            throw new IllegalArgumentException("Cannot register a config screen for '" + modId + "': no mod with that id is loaded");
        }
        if (!SLibYacl.isAvailable()) {
            LOGGER.debug("Skipping the config screen of '{}': YACL is not installed", modId);
            return;
        }

        Function<Screen, Screen> screens = Objects.requireNonNull(factory.get(), "The screen factory of '" + modId + "' is null");
        ConfigScreenFactories.add(modId, screens);
        //? if forge || neoforge && <=1.20.4 {
        /*ModList.get().getModContainerById(modId).orElseThrow().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> screens.apply(parent)));
        *///?} elif neoforge && <1.21 {
        /*ModList.get().getModContainerById(modId).orElseThrow().registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (minecraft, parent) -> screens.apply(parent));
        *///?} elif neoforge {
        /*ModList.get().getModContainerById(modId).orElseThrow().registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (container, parent) -> screens.apply(parent));
        *///?}
    }
}
