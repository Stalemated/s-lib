package com.stalemated.lib.compat.yacl.controller;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ValueFormatter;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownController;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
//? if <1.21.9
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9
//import net.minecraft.client.Minecraft;
//? if yacl: >=3.9.0 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
*///?}

import java.util.List;
import java.util.function.Consumer;

public class SelectDropdownController<T> extends AbstractDropdownController<T> {

    private final List<T> values;
    private final List<String> labels;
    private final ValueFormatter<T> formatter;

    public SelectDropdownController(Option<T> option, List<T> values, ValueFormatter<T> formatter) {
        this(option, List.copyOf(values), formatter, values.stream().map(formatter::format).map(Component::getString).toList());
    }

    private SelectDropdownController(Option<T> option, List<T> values, ValueFormatter<T> formatter, List<String> labels) {
        super(option, labels);
        this.values = values;
        this.labels = labels;
        this.formatter = formatter;
    }

    @Override
    public String getString() {
        return formatter.format(option().pendingValue()).getString();
    }

    @Override
    public void setFromString(String label) {
        if (label.equals(getString())) return;
        int index = labels.indexOf(label);
        if (index >= 0) {
            select(index);
        }
    }

    @Override
    public boolean isValueValid(String label) {
        return labels.contains(label);
    }

    @Override
    protected String getValidValue(String label, int offset) {
        return offset >= 0 && offset < labels.size() ? labels.get(offset) : getString();
    }

    private void select(int index) {
        option().requestSet(values.get(index));
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new AbstractDropdownControllerElement<T, String>(this, screen, widgetDimension) {

            @Override
            public List<String> computeMatchingValues() {
                return labels;
            }

            @Override
            public String getString(String label) {
                return label;
            }

            @Override
            //? if yacl: <3.8.0 {
            public boolean charTyped(char chr, int modifiers) {
            //?} elif yacl: <3.9.0 {
            /*public boolean onCharTyped(char chr, String cpStr, int modifiers) {
            *///?} else {
            /*public boolean charTyped(@NonNull CharacterEvent event) {
            *///?}
                return false;
            }

            @Override
            public void setFocused(boolean focused) {
                //? if yacl: <3.8.0 {
                if (focused) {
                    //? if forge {
                    /*this.focused = true;
                    *///?} else {
                    super.setFocused(true);
                    //?}
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
                if (this.isDropdownVisible()) {
                    int index = this.dropdownWidget().selectedIndex();
                    if (index >= 0 && index < labels.size()) {
                        select(index);
                        this.inputField = labels.get(index);
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
                //? if yacl: >=3.8.0
                //this.inputFieldFocused = false;
            }

            @Override
            //? if yacl: <3.8.0 {
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (!SelectDropdownController.this.option().available()) return false;
                return handleKeyPressed(this, keyCode);
            }
            //?} elif yacl: <3.9.0 {
            /*public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
                if (!SelectDropdownController.this.option().available()) return false;
                if (!this.inputFieldFocused && !this.isFocused()) return false;
                return handleKeyPressed(this, keyCode);
            }
            *///?} else {
            /*public boolean keyPressed(@NonNull KeyEvent event) {
                if (!SelectDropdownController.this.option().available()) return false;
                if (!this.inputFieldFocused && !this.isFocused()) return false;
                return handleKeyPressed(this, event.key());
            }
            *///?}

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
                if (!SelectDropdownController.this.option().available()) return false;
                return handleMouseClicked(this, mouseX, mouseY);
            }
            //?} elif yacl: <3.9.0 {
            /*public boolean onMouseClicked(double mouseX, double mouseY, int button) {
                if (!SelectDropdownController.this.option().available()) return false;
                return handleMouseClicked(this, mouseX, mouseY);
            }
            *///?} else {
            /*public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
                if (!SelectDropdownController.this.option().available()) return false;
                return handleMouseClicked(this, event.x(), event.y());
            }
            *///?}

            //? if yacl: <3.8.0 {
            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
                return false;
            }
            //?}

            //? if yacl: >=3.8.0 {
            /*@Override
            protected int getValueColor() {
                return this.isAvailable() ? -1 : this.inactiveColor;
            }
            *///?}
        };
    }

    private static boolean handleKeyPressed(AbstractDropdownControllerElement<?, ?> element, int keyCode) {
        if (element.isDropdownVisible() && element.dropdownWidget() != null) {
            switch (keyCode) {
                case InputConstants.KEY_TAB -> {
                    if (isShiftDown()) {
                        element.dropdownWidget().selectPreviousEntry();
                    } else {
                        element.dropdownWidget().selectNextEntry();
                    }
                }
                case InputConstants.KEY_DOWN -> element.dropdownWidget().selectNextEntry();
                case InputConstants.KEY_UP -> element.dropdownWidget().selectPreviousEntry();
                case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER, InputConstants.KEY_SPACE -> element.unfocus();
                case InputConstants.KEY_ESCAPE -> element.removeDropdownWidget();
                default -> {
                    return false;
                }
            }
            return true;
        }
        if (element.isFocused() && !element.isDropdownVisible() && isOpenKey(keyCode)) {
            element.createDropdownWidget();
            return true;
        }
        return false;
    }

    private static boolean isOpenKey(int keyCode) {
        return keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER || keyCode == InputConstants.KEY_SPACE;
    }

    private static boolean isShiftDown() {
        //? if <1.21.9 {
        return Screen.hasShiftDown();
        //?} else {
        /*return Minecraft.getInstance().hasShiftDown();
        *///?}
    }

    //? if yacl: <3.8.0 {
    private static boolean handleMouseClicked(AbstractDropdownControllerElement<?, ?> element, double mouseX, double mouseY) {
        if (!element.isMouseOver(mouseX, mouseY)) return false;
        element.setFocused(true);
        if (!element.isDropdownVisible()) {
            element.createDropdownWidget();
        } else {
            element.removeDropdownWidget();
        }
        return true;
    }
    //?} else {
    /*private static boolean handleMouseClicked(AbstractDropdownControllerElement<?, ?> element, double mouseX, double mouseY) {
        if (element.isMouseOver(mouseX, mouseY)) {
            if (!element.isDropdownVisible()) {
                element.createDropdownWidget();
            }
            return true;
        }
        element.unfocus();
        return false;
    }
    *///?}
}
