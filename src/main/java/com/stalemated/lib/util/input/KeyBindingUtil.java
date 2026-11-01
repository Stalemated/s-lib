package com.stalemated.lib.util.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
//? if <26.3 {
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
//?} else {
/*import org.lwjgl.sdl.SDLMouse;
*///?}

//? if fabric {
//? if <26.1 {
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
*///?}
//?}

public class KeyBindingUtil {
    public static boolean isKeyDownInGui(KeyMapping keyBinding) {
        if (keyBinding == null || keyBinding.isUnbound()) return false;

        //? if forge || neoforge {
        /*InputConstants.Key boundKey = keyBinding.getKey();
        *///?} elif <26.1 {
        InputConstants.Key boundKey = KeyBindingHelper.getBoundKeyOf(keyBinding);
        //?} else {
        /*InputConstants.Key boundKey = KeyMappingHelper.getBoundKeyOf(keyBinding);
        *///?}

        //? if <1.21.9 {
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (boundKey.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, boundKey.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, boundKey.getValue());
        //?} elif <26.3 {
        /*var window = Minecraft.getInstance().getWindow();
        if (boundKey.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window.handle(), boundKey.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, boundKey.getValue());
        *///?} else {
        /*if (boundKey.getType() == InputConstants.Type.MOUSE) {
            int button = boundKey.getValue();
            if (button < 1 || button > Integer.SIZE) return false;
            return (SDLMouse.SDL_GetMouseState(null, null) & (1 << (button - 1))) != 0;
        }
        return InputConstants.isKeyDown(boundKey.getValue());
        *///?}
    }
}
