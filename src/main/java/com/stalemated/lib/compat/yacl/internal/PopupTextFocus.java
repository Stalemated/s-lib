package com.stalemated.lib.compat.yacl.internal;

//? if >=26.3 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;

final class PopupTextFocus {
    private boolean opening;

    boolean opening() {
        return opening;
    }

    void open(GuiEventListener field, ContainerEventHandler screen, Runnable openPopup) {
        ComponentPath focusPath = screen.getCurrentFocusPath();
        opening = true;
        try {
            openPopup.run();
            if (focusPath != null && focusPath.leafComponent() == field) {
                focusPath.applyFocus(true);
            }
        } finally {
            opening = false;
        }
        Minecraft.getInstance().onTextInputFocusChange(field, true);
    }
}
*///?}
