package com.stalemated.lib.compat.yacl.internal;

import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownController;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import dev.isxander.yacl3.gui.controllers.dropdown.DropdownWidget;
//? if yacl: >=3.9.0 {
/*import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
*///?}

final class TextFieldDropdownWidget<T> extends DropdownWidget<T> {
    private final AbstractDropdownControllerElement<T, ?> field;

    TextFieldDropdownWidget(AbstractDropdownController<T> control, YACLScreen screen, Dimension<Integer> dim, AbstractDropdownControllerElement<T, ?> field) {
        super(control, screen, dim, field);
        this.field = field;
    }

    private boolean isOverList(double mouseX, double mouseY) {
        return this.dropdownDim.isPointInside((int) mouseX, (int) mouseY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isOverList(mouseX, mouseY) || this.field.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isOverList(mouseX, mouseY)) {
            super.mouseMoved(mouseX, mouseY);
        }
    }

    //? if yacl: <3.8.0 {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isOverList(mouseX, mouseY) && this.field.isMouseOver(mouseX, mouseY)) {
            return this.field.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    //?} elif yacl: <3.9.0 {
    /*@Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (!isOverList(mouseX, mouseY) && this.field.isMouseOver(mouseX, mouseY)) {
            return this.field.onMouseClicked(mouseX, mouseY, button);
        }
        return super.onMouseClicked(mouseX, mouseY, button);
    }
    *///?} else {
    /*@Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isOverList(event.x(), event.y()) && this.field.isMouseOver(event.x(), event.y())) {
            return this.field.mouseClicked(event, doubleClick);
        }
        return super.mouseClicked(event, doubleClick);
    }
    *///?}
}
