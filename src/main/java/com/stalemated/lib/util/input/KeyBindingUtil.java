package com.stalemated.lib.util.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.stalemated.lib.mixin.client.accessor.KeyBindingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class KeyBindingUtil {
    public static boolean isKeyDownInGui(KeyMapping keyBinding) {
        if (keyBinding == null || keyBinding.isUnbound()) return false;

        long window = Minecraft.getInstance().getWindow().getWindow();
        InputConstants.Key boundKey = ((KeyBindingAccessor) keyBinding).getBoundKey();

        if (boundKey.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, boundKey.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, boundKey.getValue());
    }
}
