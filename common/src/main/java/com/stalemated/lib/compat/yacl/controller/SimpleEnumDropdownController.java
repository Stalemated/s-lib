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
            public boolean onCharTyped(char chr, String cpStr, int modifiers) {
                return false;
            }

            @Override
            public void setFocused(boolean focused) {
                this.focused = focused;
                this.inputFieldFocused = focused;

                if (!focused && this.isDropdownVisible()) {
                    this.removeDropdownWidget();
                }
            }

            @Override
            public void unfocus() {
                if (this.isDropdownVisible()) {
                    int index = this.dropdownWidget().selectedIndex();
                    if (this.matchingValues == null) this.matchingValues = this.computeMatchingValues();

                    if (index >= 0 && index < this.matchingValues.size())  {
                        this.inputField = this.getString(this.matchingValues.get(index));
                        SimpleEnumDropdownController.this.setFromString(this.inputField);
                    }
                    this.removeDropdownWidget();
                }

                this.inputFieldFocused = false;
                this.renderOffset = 0;
            }

            @Override
            public void removeDropdownWidget() {
                this.screen.clearPopupControllerWidget();
                this.dropdownVisible = false;
                this.dropdownWidget = null;
                this.inputFieldFocused = false;
            }

            @Override
            public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
                if (!this.inputFieldFocused && !this.isFocused()) return false;

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
            public boolean onMouseClicked(double mouseX, double mouseY, int button) {
                return DropdownUIHelper.handleMouseClicked(this, mouseX, mouseY);
            }

            @Override
            protected int getValueColor() {
                return this.isAvailable() ? -1 : this.inactiveColor;
            }
        };
    }
}