package com.stalemated.lib.compat.yacl.controller;

import com.stalemated.lib.util.color.ColorUtils;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ColorController;
import java.awt.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

//? if yacl: <3.9.0 && <1.21.11{
import net.minecraft.client.gui.screens.Screen;
//?} elif yacl: <3.9.0 {
/*import net.minecraft.client.input.KeyEvent;
*///?} else {
/*import dev.isxander.yacl3.gui.controllers.string.IStringController;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
*///?}

//? if yacl: <3.9.0 {
public class AdvancedColorController extends ColorController {
//?} else {
/*public class AdvancedColorController implements IStringController<Color> {
    private final ColorController delegate;
*///?}
    private final Option<String> stringOption;
    private final boolean alpha;

    //? if yacl: <3.9.0 {
    public AdvancedColorController(Option<String> stringOption, boolean alpha) {
        super(Option.<Color>createBuilder()
                .name(stringOption.name())
                .binding(
                        ColorUtils.parseToAWT(stringOption.pendingValue()),
                        () -> ColorUtils.parseToAWT(stringOption.pendingValue()),
                        val -> {}
                )
                .controller(ColorControllerBuilder::create)
                .available(stringOption.available())
                .build(), alpha);
        this.stringOption = stringOption;
        this.alpha = alpha;
    //?} else {
    /*public AdvancedColorController(Option<String> stringOption, boolean alpha) {
        this.stringOption = stringOption;
        this.alpha = alpha;
        this.delegate = new ColorController(Option.<Color>createBuilder()
                .name(stringOption.name())
                .binding(
                        ColorUtils.parseToAWT(stringOption.pendingValue()),
                        () -> ColorUtils.parseToAWT(stringOption.pendingValue()),
                        val -> {}
                )
                .controller(ColorControllerBuilder::create)
                .available(stringOption.available())
                .build(), alpha);
    *///?}

        this.option().addEventListener((opt, event) -> {
            Color pickerColor = opt.pendingValue();
            Color currentColor = ColorUtils.parseToAWT(this.stringOption.pendingValue());
            if (!pickerColor.equals(currentColor)) {
                String hex;
                if (this.alpha) {
                    hex = String.format("#%02X%02X%02X%02X", pickerColor.getAlpha(), pickerColor.getRed(), pickerColor.getGreen(), pickerColor.getBlue());
                } else {
                    hex = String.format("#%02X%02X%02X", pickerColor.getRed(), pickerColor.getGreen(), pickerColor.getBlue());
                }
                this.stringOption.requestSet(hex);
            }
        });

        this.stringOption.addEventListener((opt, event) -> {
            Color parsed = ColorUtils.parseToAWT(opt.pendingValue());
            if (!parsed.equals(this.option().pendingValue())) {
                this.option().requestSet(parsed);
            }
        });
    }

    @Override
    public String getString() {
        return this.stringOption.pendingValue();
    }

    @Override
    public void setFromString(String value) {
        this.stringOption.requestSet(value);

        Color parsed = ColorUtils.parseToAWT(value);
        if (!parsed.equals(this.option().pendingValue())) {
            this.option().requestSet(parsed);
        }
    }

    @Override
    public Component formatValue() {
        return Component.literal(getString());
    }

    //? if yacl: >=3.9.0 {
    /*@Override
    public Option<Color> option() {
        return this.delegate.option();
    }

    public boolean allowAlpha() {
        return this.alpha;
    }
    *///?}

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new AdvancedColorControllerElement(this, screen, widgetDimension);
    }

    //? if yacl: <3.9.0 {
    public class AdvancedColorControllerElement extends ColorControllerElement {
        public AdvancedColorControllerElement(AdvancedColorController control, YACLScreen screen, Dimension<Integer> dim) {
            super(control, screen, dim);
    //?} else {
    /*public class AdvancedColorControllerElement extends ColorController.ColorControllerElement {
        public AdvancedColorControllerElement(AdvancedColorController control, YACLScreen screen, Dimension<Integer> dim) {
            super(control.delegate, screen, dim);
    *///?}

            AdvancedColorController.this.stringOption.addEventListener((opt, event) -> {
                String newVal = opt.pendingValue();
                if (!newVal.equals(this.inputField)) {
                    this.inputField = newVal;
                    this.caretPos = this.inputField.length();
                }
            });
        }

        @Override
        public void write(String string) {
            if (!AdvancedColorController.this.stringOption.available()) return;
            if (this.modifyInput(builder -> {
                if (this.selectionLength != 0) {
                    int start = Math.min(this.caretPos, this.caretPos + this.selectionLength);
                    int end = Math.max(this.caretPos, this.caretPos + this.selectionLength);
                    builder.delete(start, end);
                    this.caretPos = start;
                }
                builder.insert(this.caretPos, string);
            })) {
                this.caretPos += string.length();
                this.selectionLength = 0;
                this.updateControl();
            }
        }

        @Override
        protected void doBackspace() {
            if (!AdvancedColorController.this.stringOption.available()) return;
            if (this.selectionLength != 0) {
                this.doDelete();
                return;
            }
            if (this.caretPos > 0 && this.modifyInput(builder -> builder.deleteCharAt(this.caretPos - 1))) {
                this.caretPos--;
                this.updateControl();
            }
        }

        @Override
        protected void doDelete() {
            if (!AdvancedColorController.this.stringOption.available()) return;
            if (this.selectionLength != 0) {
                if (this.modifyInput(builder -> {
                    int start = Math.min(this.caretPos, this.caretPos + this.selectionLength);
                    int end = Math.max(this.caretPos, this.caretPos + this.selectionLength);
                    builder.delete(start, end);
                })) {
                    this.caretPos = Math.min(this.caretPos, this.caretPos + this.selectionLength);
                    this.selectionLength = 0;
                    this.updateControl();
                }
                return;
            }
            if (this.caretPos < this.inputField.length() && this.modifyInput(builder -> builder.deleteCharAt(this.caretPos))) {
                this.updateControl();
            }
        }

        @Override protected boolean doCut() {
            if (!AdvancedColorController.this.stringOption.available()) return false;
            if (this.selectionLength != 0) { this.doCopy(); this.doDelete(); return true; } return false;
        }

        @Override protected boolean doCopy() {
            if (this.selectionLength != 0) {
                int start = Math.min(this.caretPos, this.caretPos + this.selectionLength);
                int end = Math.max(this.caretPos, this.caretPos + this.selectionLength);
                Minecraft.getInstance().keyboardHandler.setClipboard(this.inputField.substring(start, end));
                return true;
            }
            return false;
        }

        @Override protected boolean doSelectAll() {
            this.caretPos = this.inputField.length(); this.selectionLength = -this.inputField.length(); return true;
        }

        @Override protected void setSelectionLength() {  }

        @Override protected int getDefaultCaretPos() { return this.inputField.length(); }

        private boolean handleKeyAction(int keyCode, boolean isSelectAll, boolean isCopy, boolean isPaste, boolean isCut) {
            if (!this.inputFieldFocused || !AdvancedColorController.this.stringOption.available()) return false;
            
            if (isSelectAll) { this.doSelectAll(); return true; }
            if (isCopy) { this.doCopy(); return true; }
            if (isPaste) { this.write(Minecraft.getInstance().keyboardHandler.getClipboard()); return true; }
            if (isCut) { this.doCut(); return true; }

            switch (keyCode) {
                case 259:
                    this.doBackspace();
                    return true;
                case 261:
                    this.doDelete();
                    return true;
                case 263: // Left
                    if (this.caretPos > 0) this.caretPos--;
                    this.selectionLength = 0;
                    return true;
                case 262: // Right
                    if (this.caretPos < this.inputField.length()) this.caretPos++;
                    this.selectionLength = 0;
                    return true;
                case 268: // Start
                    this.caretPos = 0;
                    this.selectionLength = 0;
                    return true;
                case 269: // End
                    this.caretPos = this.inputField.length();
                    this.selectionLength = 0;
                    return true;
            }
            return false;
        }

        //? if yacl: <3.8.0 {
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return handleKeyAction(keyCode, Screen.isSelectAll(keyCode), Screen.isCopy(keyCode), Screen.isPaste(keyCode), Screen.isCut(keyCode));
        }
        //?} elif <1.21.11 {
        /*@Override
        public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
            return handleKeyAction(keyCode, Screen.isSelectAll(keyCode), Screen.isCopy(keyCode), Screen.isPaste(keyCode), Screen.isCut(keyCode));
        }
        *///?} elif yacl: <3.9.0 {
        /*@Override
        public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
            KeyEvent event = new KeyEvent(keyCode, scanCode, modifiers);
            return handleKeyAction(keyCode, event.isSelectAll(), event.isCopy(), event.isPaste(), event.isCut());
        }
        *///?} else {
        /*@Override
        public boolean keyPressed(KeyEvent event) {
            return handleKeyAction(event.key(), event.isSelectAll(), event.isCopy(), event.isPaste(), event.isCut());
        }
        *///?}

        //? if yacl: >=3.8.0 {
        /*private void handleInputClick(double mouseX, double mouseY) {
            if (this.inputFieldBounds != null && this.inputFieldBounds.isPointInside((int) mouseX, (int) mouseY) && !this.isMouseOverColorPreview(mouseX, mouseY)) {
                this.setFocused(true);
                int clickOffset = (int) mouseX - this.inputFieldBounds.x();
                String renderedText = this.textRenderer.plainSubstrByWidth(this.inputField, this.inputFieldBounds.width() * 2);
                this.caretPos = this.textRenderer.plainSubstrByWidth(renderedText, clickOffset).length();

                int clickX = this.inputFieldBounds.x();
                int bestCaret = 0;
                int minDistance = Integer.MAX_VALUE;

                for (int i = 0; i <= this.inputField.length(); i++) {
                    int charX = clickX + this.textRenderer.width(this.inputField.substring(0, i));
                    int distance = Math.abs(charX - (int) mouseX);
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestCaret = i;
                    }
                }

                this.caretPos = bestCaret;
                this.selectionLength = 0;
            }
        }
        *///?}

        //? if yacl: <3.8.0 {
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!AdvancedColorController.this.stringOption.available()) return false;
            boolean handled = super.mouseClicked(mouseX, mouseY, button);
            if (handled) {
                if (this.inputField.isEmpty()) {
                    this.caretPos = 0;
                } else if (this.caretPos == 1 && mouseX < this.getDimension().x() + this.getXPadding() + 4) {
                    this.caretPos = 0;
                }
            }
            return handled;
        }
        //?} elif yacl: <3.9.0 {
        /*@Override
        public boolean onMouseClicked(double mouseX, double mouseY, int button) {
            if (!AdvancedColorController.this.stringOption.available()) return false;
            boolean handled = super.onMouseClicked(mouseX, mouseY, button);
            handleInputClick(mouseX, mouseY);
            return handled;
        }
        *///?} else {
        /*@Override
        public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
            if (!AdvancedColorController.this.stringOption.available()) return false;
            boolean handled = super.mouseClicked(event, doubleClick);
            handleInputClick(event.x(), event.y());
            return handled;
        }
        *///?}
    }
}
