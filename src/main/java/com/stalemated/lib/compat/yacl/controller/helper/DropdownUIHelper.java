package com.stalemated.lib.compat.yacl.controller.helper;

import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import net.minecraft.client.gui.screens.Screen;

//? if >=1.21.1
//import net.minecraft.client.Minecraft;
//? if yacl: >=3.9.0 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

public class DropdownUIHelper {

    public static boolean handleKeyPressed(AbstractDropdownControllerElement<?, ?> element, int keyCode) {
        if (element.isDropdownVisible() && element.dropdownWidget() != null) {
            switch (keyCode) {
                case 258: // Tab
                    //? if <1.21.11 {
                    if (Screen.hasShiftDown()) {
                    //?} else {
                    /*if (Minecraft.getInstance().hasShiftDown()) {
                    *///?}
                        element.dropdownWidget().selectPreviousEntry();
                    } else {
                        element.dropdownWidget().selectNextEntry();
                    }
                    return true;
                case 264: // Down Arrow
                    element.dropdownWidget().selectNextEntry();
                    return true;
                case 265: // Up Arrow
                    element.dropdownWidget().selectPreviousEntry();
                    return true;
                case 257: // Enter
                case 335: // Numpad Enter
                case 32:  // Space
                    element.unfocus();
                    return true;
                case 256: // Esc
                    element.removeDropdownWidget();
                    return true;
            }
        } else if (element.isFocused() && !element.isDropdownVisible()) {
            switch (keyCode) {
                case 257: // Enter
                case 335: // Numpad Enter
                case 32:  // Space
                    element.createDropdownWidget();
                    return true;
            }
        }
        return false;
    }

    //? if yacl: <3.8.0 {
    public static boolean handleMouseClicked(AbstractDropdownControllerElement<?, ?> element, double mouseX, double mouseY) {
        if (element.isMouseOver(mouseX, mouseY)) {
            element.setFocused(true);
            if (!element.isDropdownVisible()) element.createDropdownWidget();
            else element.removeDropdownWidget();
            return true;
        }
        return false;
    }
    //?} else {
    /*public static boolean handleMouseClicked(AbstractDropdownControllerElement<?, ?> element, double mouseX, double mouseY) {
        if (element.isMouseOver(mouseX, mouseY)) {
            if (!element.isDropdownVisible()) {
                element.createDropdownWidget();
            }
            return true;
        } else {
            element.unfocus();
        }

        return false;
    }
    *///?}

    //? if yacl: >=3.9.0 {
    /*public static boolean handleKeyPressed(AbstractDropdownControllerElement<?, ?> element, KeyEvent event) {
        return handleKeyPressed(element, event.key());
    }

    public static boolean handleMouseClicked(AbstractDropdownControllerElement<?, ?> element, MouseButtonEvent event) {
        return handleMouseClicked(element, event.x(), event.y());
    }
    *///?}
}
