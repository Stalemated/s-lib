package com.stalemated.lib.compat.yacl.controller.helper;

import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import net.minecraft.client.gui.screen.Screen;

public class DropdownUIHelper {

    public static boolean handleKeyPressed(AbstractDropdownControllerElement<?, ?> element, int keyCode) {
        if (element.isDropdownVisible() && element.dropdownWidget() != null) {
            switch (keyCode) {
                case 258: // Tab
                    if (Screen.hasShiftDown()) {
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

    public static boolean handleMouseClicked(AbstractDropdownControllerElement<?, ?> element, double mouseX, double mouseY) {
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
}