package com.stalemated.lib.util.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.stalemated.lib.mixin.client.accessor.KeyBindingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
//? if <26.3
import org.lwjgl.glfw.GLFW;

public class KeyBindingUtil {
    public static boolean isKeyDownInGui(KeyMapping keyBinding) {
        if (keyBinding == null || keyBinding.isUnbound()) return false;

        InputConstants.Key boundKey = ((KeyBindingAccessor) keyBinding).getBoundKey();

        //? if <1.21.11 {
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
            int btn = boundKey.getValue();
            var mouse = Minecraft.getInstance().mouseHandler;
            if (btn == 0 || btn == 1) return mouse.isLeftPressed();
            if (btn == 2) return mouse.isMiddlePressed();
            if (btn == 3) return mouse.isRightPressed();
            return false;
        }
        return InputConstants.isKeyDown(boundKey.getValue());
        *///?}
    }
}
