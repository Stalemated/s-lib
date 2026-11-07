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
/^import net.neoforged.neoforge.client.ConfigScreenHandler;
^///?} else {
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
//?}
*///?}

public final class SLibConfigScreens {

    private SLibConfigScreens() {}

    public static void register(String modId, Supplier<Function<Screen, Screen>> factory) {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(factory, "factory cannot be null");
        if (!SLibYacl.isAvailable()) {
            LOGGER.debug("Skipping the config screen of '{}': YACL is not installed", modId);
            return;
        }
        if (!PlatformHelper.isModLoaded(modId)) {
            throw new IllegalArgumentException("Cannot register a config screen for '" + modId + "': no mod with that id is loaded");
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
