package com.stalemated.lib.compat.yacl.controller;

import com.stalemated.lib.compat.yacl.controller.helper.DropdownUIHelper;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ValueFormatter;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.EnumDropdownController;
import dev.isxander.yacl3.gui.controllers.dropdown.EnumDropdownControllerElement;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class SimpleEnumDropdownController<E extends Enum<E>> extends EnumDropdownController<E> {

    public SimpleEnumDropdownController(Option<E> option, ValueFormatter<E> formatter) {
        super(option, formatter);
    }

    @Override
    protected @NotNull Stream<String> getValidEnumConstants(String value) {
        return this.getAllowedValues().stream();
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new EnumDropdownControllerElement<>(this, screen, widgetDimension) {
            @Override
            public boolean charTyped(char chr, int modifiers) {
                return false;
            }

            @Override
            public void setFocused(boolean focused) {
                if (focused) {
                    super.setFocused(true);
                    this.inputFieldFocused = false;
                } else {
                    this.unfocus();
                }
            }

            @Override
            public void unfocus() {
                if (this.isDropdownVisible()) {
                    int index = this.dropdownWidget().selectedIndex();
                    if (index >= 0 && index < SimpleEnumDropdownController.this.getAllowedValues().size()) {
                        this.inputField = SimpleEnumDropdownController.this.getAllowedValues().get(index);
                    }
                    this.removeDropdownWidget();
                }
                super.unfocus();
            }

            @Override
            public void removeDropdownWidget() {
                this.screen.clearPopupControllerWidget();
                this.dropdownVisible = false;
                this.dropdownWidget = null;
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                return DropdownUIHelper.handleKeyPressed(this, keyCode);
            }

            @Override
            public boolean modifyInput(Consumer<StringBuilder> builder) {
                return false;
            }

            @Override
            public boolean doSelectAll() {
                return false;
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return DropdownUIHelper.handleMouseClicked(this, mouseX, mouseY);
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
                return false;
            }
        };
    }
}