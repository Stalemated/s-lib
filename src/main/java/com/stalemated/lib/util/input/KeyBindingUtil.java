package com.stalemated.lib.util.input;

import com.stalemated.lib.mixin.client.accessor.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindingUtil {
    public static boolean isKeyDownInGui(KeyBinding keyBinding) {
        if (keyBinding == null || keyBinding.isUnbound()) return false;

        long window = MinecraftClient.getInstance().getWindow().getHandle();
        InputUtil.Key boundKey = ((KeyBindingAccessor) keyBinding).getBoundKey();

        if (boundKey.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, boundKey.getCode()) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(window, boundKey.getCode());
    }
}
