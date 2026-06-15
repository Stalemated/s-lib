package com.stalemated.lib.compat.yacl.controller;

import com.stalemated.lib.compat.yacl.controller.helper.DropdownUIHelper;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ValueFormatter;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownController;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public class SimpleStringDropdownController extends AbstractDropdownController<String> {
    private final List<String> rawValues;
    private final ValueFormatter<String> formatter;

    public SimpleStringDropdownController(Option<String> option, List<String> rawValues, ValueFormatter<String> formatter) {
        super(option, rawValues.stream().map(formatter::format).map(Text::getString).toList());
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
                    if (index >= 0 && index < SimpleStringDropdownController.this.getAllowedValues().size()) {
                        this.inputField = SimpleStringDropdownController.this.getAllowedValues().get(index);
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
            public boolean modifyInput(Consumer<StringBuilder> builder) { return false; }

            @Override
            public boolean doSelectAll() {
                return false;
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return DropdownUIHelper.handleMouseClicked(this, mouseX, mouseY);
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) { return false; }
        };
    }
}