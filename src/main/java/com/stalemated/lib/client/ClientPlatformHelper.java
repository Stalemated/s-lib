package com.stalemated.lib.client;

import net.minecraft.client.KeyMapping;

//? if fabric {
//? if <26.1 {
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
*///?}
//?}

//? if forge || neoforge {
/*import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
*///?}

/**
 * Client helpers that depend on the mod loader. For now, only key mapping registration.
 * Client only.
 */
public final class ClientPlatformHelper {

    //? if forge || neoforge {
    /*private static final List<KeyMapping> PENDING_KEY_MAPPINGS = new CopyOnWriteArrayList<>();
    private static volatile boolean keyMappingsRegistered;
    *///?}

    private ClientPlatformHelper() {}

    /**
     * Registers a {@link KeyMapping} with the current mod loader. Unlike the Fabric API, it does
     * not return the key mapping.
     * <p>
     * On Fabric, call this from the client initializer. On Forge and NeoForge, call this during
     * mod construction, before {@code RegisterKeyMappingsEvent} fires.
     *
     * @param keyBinding the key mapping to register
     * @throws IllegalStateException on Forge and NeoForge, if called after {@code RegisterKeyMappingsEvent}
     */
    public static void registerKeyBinding(KeyMapping keyBinding) {
        //? if fabric && <26.1 {
        KeyBindingHelper.registerKeyBinding(keyBinding);
        //?} elif fabric {
        /*KeyMappingHelper.registerKeyMapping(keyBinding);
        *///?} elif forge || neoforge {
        /*if (keyMappingsRegistered) throw new IllegalStateException("Key mapping '" + keyBinding.getName() + "' was registered after RegisterKeyMappingsEvent; register it from the mod constructor");
        PENDING_KEY_MAPPINGS.add(keyBinding);
        *///?}
    }

    //? if forge || neoforge {
    /*static void registerPendingKeyMappings(Consumer<KeyMapping> register) {
        keyMappingsRegistered = true;
        PENDING_KEY_MAPPINGS.forEach(register);
    }
    *///?}
}
