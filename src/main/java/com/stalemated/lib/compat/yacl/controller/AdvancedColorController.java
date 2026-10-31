package com.stalemated.lib.compat.yacl.controller;

import com.mojang.blaze3d.platform.InputConstants;
import com.stalemated.lib.util.color.ColorUtils;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ColorController;
import dev.isxander.yacl3.gui.controllers.string.IStringController;
import java.awt.Color;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;

//? if yacl: <3.9.0 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
*///?}
//? if <1.21.9 {
import net.minecraft.client.gui.screens.Screen;
//?} else {
/*import net.minecraft.client.input.KeyEvent;
*///?}

public class AdvancedColorController implements IStringController<String> {
    private final Option<String> option;
    private final boolean allowAlpha;

    public AdvancedColorController(Option<String> option, boolean allowAlpha) {
        this.option = option;
        this.allowAlpha = allowAlpha;
    }

    @Override
    public Option<String> option() {
        return option;
    }

    @Override
    public String getString() {
        return option.pendingValue();
    }

    @Override
    public void setFromString(String text) {
        if (parse(text) != null) {
            option.requestSet(text);
        }
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new AdvancedColorControllerElement(new ColorController(pickerOption(), allowAlpha), screen, widgetDimension);
    }

    private Option<Color> pickerOption() {
        return Option.<Color>createBuilder()
                .name(option.name())
                .binding(currentColor(), this::currentColor, picked -> {})
                .controller(ColorControllerBuilder::create)
                .build();
    }

    private Color currentColor() {
        String value = getString();
        Color color = parse(value);
        if (color == null) {
            throw new IllegalStateException("Color option holds '" + value + "', which is not a color");
        }
        return color;
    }

    private Color parse(String text) {
        if (allowAlpha) {
            return ColorUtils.parseArgbColor(text);
        }
        TextColor textColor = ColorUtils.parseTextColor(text);
        return textColor != null ? new Color(textColor.getValue()) : null;
    }

    private class AdvancedColorControllerElement extends ColorController.ColorControllerElement {
        private final Option<Color> picker;
        private String shownValue;
        private Color pickerColor;

        AdvancedColorControllerElement(ColorController pickerController, YACLScreen screen, Dimension<Integer> dim) {
            super(pickerController, screen, dim);
            this.picker = pickerController.option();
            this.shownValue = getString();
            this.pickerColor = picker.pendingValue();
            this.inputField = shownValue;
        }

        //? if yacl: <3.9.0 {
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            syncWithOption();
            super.render(graphics, mouseX, mouseY, delta);
        }
        //?} else {
        /*@Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            syncWithOption();
            super.extractRenderState(graphics, mouseX, mouseY, a);
        }
        *///?}

        private void syncWithOption() {
            Color picked = picker.pendingValue();
            if (!picked.equals(pickerColor)) {
                setFromString(ColorUtils.toArgbHex(picked));
            }

            String value = getString();
            boolean changed = !value.equals(shownValue);
            if (changed) {
                shownValue = value;
                pickerColor = currentColor();
                setPickerColorKeepingInput(pickerColor);
            }
            if (changed || !inputFieldFocused) {
                show(value);
            }
        }

        private void setPickerColorKeepingInput(Color color) {
            String input = inputField;
            picker.requestSet(color);
            inputField = input;
        }

        private void show(String value) {
            if (inputField.equals(value)) return;
            inputField = value;
            caretPos = value.length();
            selectionLength = 0;
            checkRenderOffset();
        }

        @Override
        protected Component getValueText() {
            MutableComponent text = Component.literal(inputField);
            return parse(inputField) != null ? text : text.withStyle(ChatFormatting.RED);
        }

        @Override
        protected boolean isAvailable() {
            return option.available();
        }

        @Override
        public boolean modifyInput(Consumer<StringBuilder> edit) {
            return isAvailable() && super.modifyInput(edit);
        }

        @Override
        protected void updateControl() {
            setFromString(inputField);
        }

        @Override
        public void write(String text) {
            int start = getSelectionStart();
            int end = getSelectionEnd();
            if (modifyInput(builder -> builder.replace(start, end, text))) {
                caretPos = start + text.length();
                selectionLength = 0;
                checkRenderOffset();
            }
        }

        @Override
        protected void doBackspace() {
            if (selectionLength != 0) {
                write("");
            } else if (caretPos > 0 && modifyInput(builder -> builder.deleteCharAt(caretPos - 1))) {
                caretPos--;
                checkRenderOffset();
            }
        }

        @Override
        protected void doDelete() {
            if (selectionLength != 0) {
                write("");
            } else if (caretPos < inputField.length()) {
                modifyInput(builder -> builder.deleteCharAt(caretPos));
            }
        }

        @Override
        protected boolean doCopy() {
            if (selectionLength == 0) return false;
            client.keyboardHandler.setClipboard(getSelection());
            return true;
        }

        @Override
        protected boolean doCut() {
            if (!doCopy()) return false;
            write("");
            return true;
        }

        @Override
        protected boolean doSelectAll() {
            caretPos = inputField.length();
            selectionLength = -caretPos;
            checkRenderOffset();
            return true;
        }

        @Override
        protected int getDefaultCaretPos() {
            return inputField.length();
        }

        private void moveCaret(int position) {
            caretPos = Mth.clamp(position, 0, inputField.length());
            selectionLength = 0;
            checkRenderOffset();
        }

        private boolean handleKey(int key, boolean selectAll, boolean copy, boolean paste, boolean cut) {
            if (!inputFieldFocused) return false;
            if (selectAll) return doSelectAll();
            if (copy) return doCopy();
            if (paste) return doPaste();
            if (cut) return doCut();

            switch (key) {
                case InputConstants.KEY_ESCAPE, InputConstants.KEY_RETURN -> unfocus();
                case InputConstants.KEY_BACKSPACE -> doBackspace();
                case InputConstants.KEY_DELETE -> doDelete();
                case InputConstants.KEY_LEFT -> moveCaret(caretPos - 1);
                case InputConstants.KEY_RIGHT -> moveCaret(caretPos + 1);
                case InputConstants.KEY_HOME -> moveCaret(0);
                case InputConstants.KEY_END -> moveCaret(inputField.length());
                default -> {
                    return false;
                }
            }
            return true;
        }

        //? if yacl: <3.8.0 {
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return handleKey(keyCode, Screen.isSelectAll(keyCode), Screen.isCopy(keyCode), Screen.isPaste(keyCode), Screen.isCut(keyCode));
        }
        //?} elif <1.21.9 {
        /*@Override
        public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
            return handleKey(keyCode, Screen.isSelectAll(keyCode), Screen.isCopy(keyCode), Screen.isPaste(keyCode), Screen.isCut(keyCode));
        }
        *///?} elif yacl: <3.9.0 {
        /*@Override
        public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
            KeyEvent event = new KeyEvent(keyCode, scanCode, modifiers);
            return handleKey(keyCode, event.isSelectAll(), event.isCopy(), event.isPaste(), event.isCut());
        }
        *///?} else {
        /*@Override
        public boolean keyPressed(@NonNull KeyEvent event) {
            return handleKey(event.key(), event.isSelectAll(), event.isCopy(), event.isPaste(), event.isCut());
        }
        *///?}

        private boolean click(double mouseX, double mouseY, BooleanSupplier colorPreviewClick) {
            if (!isAvailable()) return false;
            if (isMouseOverColorPreview(mouseX, mouseY)) {
                boolean handled = colorPreviewClick.getAsBoolean();
                selectionLength = 0;
                return handled;
            }
            caretPos = inputFieldBounds.isPointInside((int) mouseX, (int) mouseY) ? caretAt(mouseX) : getDefaultCaretPos();
            selectionLength = 0;
            return true;
        }

        private int caretAt(double mouseX) {
            int textX = inputFieldBounds.xLimit() - textRenderer.width(getValueText()) + renderOffset;
            return textRenderer.plainSubstrByWidth(inputField, (int) mouseX - textX).length();
        }

        //? if yacl: <3.8.0 {
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return click(mouseX, mouseY, () -> super.mouseClicked(mouseX, mouseY, button));
        }
        //?} elif yacl: <3.9.0 {
        /*@Override
        public boolean onMouseClicked(double mouseX, double mouseY, int button) {
            return click(mouseX, mouseY, () -> super.onMouseClicked(mouseX, mouseY, button));
        }
        *///?} else {
        /*@Override
        public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
            return click(event.x(), event.y(), () -> super.mouseClicked(event, doubleClick));
        }
        *///?}
    }
}
