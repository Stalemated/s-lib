package com.stalemated.lib.compat.yacl.controller;

import com.stalemated.lib.compat.yacl.controller.helper.DropdownUIHelper;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ValueFormatter;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownController;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public class SimpleStringDropdownController extends AbstractDropdownController<String> {
    private final List<String> rawValues;
    private final ValueFormatter<String> formatter;

    public SimpleStringDropdownController(Option<String> option, List<String> rawValues, ValueFormatter<String> formatter) {
        super(option, rawValues.stream().map(formatter::format).map(Component::getString).toList());
        this.rawValues = rawValues;
        this.formatter = formatter;
    }

    @Override
    public String getString() {
        return this.formatter.format(this.option().pendingValue()).getString();
    }

    @Override
    public void setFromString(String value) {
        String lowerVal = value.toLowerCase();
        for (String raw : this.rawValues) {
            if (this.formatter.format(raw).getString().toLowerCase().equals(lowerVal)) {
                this.option().requestSet(raw);
                return;
            }
        }
        this.option().requestSet(this.option().pendingValue());
    }

    //? if yacl: <3.8.0 {
    @Override
    public boolean isValueValid(String value) {
        String lowerVal = value.toLowerCase();
        for (String constant : this.getAllowedValues()) {
            if (constant.toLowerCase().equals(lowerVal)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getValidValue(String value, int offset) {
        return this.getAllowedValues().stream().skip(offset).findFirst().orElseGet(this::getString);
    }
    //?}

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new AbstractDropdownControllerElement<String, String>(this, screen, widgetDimension) {

            @Override
            public List<String> computeMatchingValues() {
                return SimpleStringDropdownController.this.getAllowedValues();
            }

            @Override
            public String getString(String value) {
                return value;
            }

            @Override
            //? if yacl: <3.8.0 {
            public boolean charTyped(char chr, int modifiers) {
            //?} else {
            /*public boolean onCharTyped(char chr, String cpStr, int modifiers) {
             *///?}
                return false;
            }

            @Override
            public void setFocused(boolean focused) {
                //? if yacl: <3.8.0 {
                if (focused) {
                    super.setFocused(true);
                    this.inputFieldFocused = false;
                } else {
                    this.unfocus();
                }
                //?} else {
                /*this.focused = focused;
                this.inputFieldFocused = focused;

                if (!focused && this.isDropdownVisible()) {
                    this.removeDropdownWidget();
                }
                *///?}
            }

            @Override
            public void unfocus() {
                //? if yacl: <3.8.0 {
                if (this.isDropdownVisible()) {
                    int index = this.dropdownWidget().selectedIndex();
                    if (index >= 0 && index < SimpleStringDropdownController.this.getAllowedValues().size()) {
                        this.inputField = SimpleStringDropdownController.this.getAllowedValues().get(index);
                    }
                    this.removeDropdownWidget();
                }
                super.unfocus();
                //?} else {
                /*if (this.isDropdownVisible()) {
                    int index = this.dropdownWidget().selectedIndex();
                    if (this.matchingValues == null) this.matchingValues = this.computeMatchingValues();

                    if (index >= 0 && index < this.matchingValues.size()) {
                        this.inputField = this.getString(this.matchingValues.get(index));
                        SimpleStringDropdownController.this.setFromString(this.inputField);
                    }
                    this.removeDropdownWidget();
                }

                this.inputFieldFocused = false;
                this.renderOffset = 0;
                *///?}
            }

            @Override
            public void removeDropdownWidget() {
                this.screen.clearPopupControllerWidget();
                this.dropdownVisible = false;
                this.dropdownWidget = null;
                //? if yacl: >=3.8.0
                //this.inputFieldFocused = false;
            }

            @Override
            //? if yacl: <3.8.0 {
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            //?} else {
            /*public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
             *///?}
                if (!SimpleStringDropdownController.this.option().available()) return false;
                //? if yacl: >=3.8.0
                //if (!this.inputFieldFocused && !this.isFocused()) return false;

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
            //? if yacl: <3.8.0 {
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
            //?} else {
            /*public boolean onMouseClicked(double mouseX, double mouseY, int button) {
             *///?}
                if (!SimpleStringDropdownController.this.option().available()) return false;
                return DropdownUIHelper.handleMouseClicked(this, mouseX, mouseY);
            }

            //? if yacl: <3.8.0 {
            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
                return false;
            }
            //?}
        };
    }
}
