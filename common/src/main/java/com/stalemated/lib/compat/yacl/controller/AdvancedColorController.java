package com.stalemated.lib.compat.yacl.controller;

import com.stalemated.lib.util.color.ColorUtils;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ColorController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;

public class AdvancedColorController extends ColorController {
    private final Option<String> stringOption;
    private final boolean alpha;

    public AdvancedColorController(Option<String> stringOption, boolean alpha) {
        super(Option.<Color>createBuilder()
                .name(stringOption.name())
                .binding(
                        ColorUtils.parseToAWT(stringOption.pendingValue()),
                        () -> ColorUtils.parseToAWT(stringOption.pendingValue()),
                        val -> {}
                )
                .controller(ColorControllerBuilder::create)
                .build(), alpha);
        this.stringOption = stringOption;
        this.alpha = alpha;

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
    public Text formatValue() {
        return Text.literal(getString());
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new AdvancedColorControllerElement(this, screen, widgetDimension);
    }

    public class AdvancedColorControllerElement extends ColorControllerElement {
        public AdvancedColorControllerElement(AdvancedColorController control, YACLScreen screen, Dimension<Integer> dim) {
            super(control, screen, dim);

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
            if (this.selectionLength != 0) { this.doCopy(); this.doDelete(); return true; } return false;
        }

        @Override protected boolean doCopy() {
            if (this.selectionLength != 0) {
                int start = Math.min(this.caretPos, this.caretPos + this.selectionLength);
                int end = Math.max(this.caretPos, this.caretPos + this.selectionLength);
                MinecraftClient.getInstance().keyboard.setClipboard(this.inputField.substring(start, end));
                return true;
            }
            return false;
        }

        @Override protected boolean doSelectAll() {
            this.caretPos = this.inputField.length(); this.selectionLength = -this.inputField.length(); return true;
        }

        @Override protected void setSelectionLength() {  }

        @Override protected int getDefaultCaretPos() { return this.inputField.length(); }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!this.inputFieldFocused) return false;

            if (Screen.isSelectAll(keyCode)) { this.doSelectAll(); return true; }
            if (Screen.isCopy(keyCode)) { this.doCopy(); return true; }
            if (Screen.isPaste(keyCode)) { this.write(MinecraftClient.getInstance().keyboard.getClipboard()); return true; }
            if (Screen.isCut(keyCode)) { this.doCut(); return true; }

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

        @Override
        public boolean onMouseClicked(double mouseX, double mouseY, int button) {
            boolean handled = super.onMouseClicked(mouseX, mouseY, button);

            if (this.inputFieldBounds != null && this.inputFieldBounds.isPointInside((int) mouseX, (int) mouseY) && !this.isMouseOverColorPreview(mouseX, mouseY)) {
                this.setFocused(true);
                int clickOffset = (int) mouseX - this.inputFieldBounds.x();
                String renderedText = this.textRenderer.trimToWidth(this.inputField, this.inputFieldBounds.width() * 2);
                this.caretPos = this.textRenderer.trimToWidth(renderedText, clickOffset).length();

                int clickX = this.inputFieldBounds.x();
                int bestCaret = 0;
                int minDistance = Integer.MAX_VALUE;

                for (int i = 0; i <= this.inputField.length(); i++) {
                    int charX = clickX + this.textRenderer.getWidth(this.inputField.substring(0, i));
                    int distance = Math.abs(charX - (int) mouseX);
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestCaret = i;
                    }
                }

                this.caretPos = bestCaret;
                this.selectionLength = 0;
            }

            return handled;
        }
    }
}