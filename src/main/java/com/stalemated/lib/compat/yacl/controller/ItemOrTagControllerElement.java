package com.stalemated.lib.compat.yacl.controller;

import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import net.minecraft.ChatFormatting;
//? if yacl: <3.9.0 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
*///?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.stalemated.lib.compat.yacl.controller.helper.ItemRegistryHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemOrTagControllerElement extends AbstractDropdownControllerElement<String, String> {
    private final ItemOrTagController itemOrTagController;
    private final Map<String, ItemStack> itemCache = new HashMap<>();
    private ItemStack currentItemIcon = ItemStack.EMPTY;

    public ItemOrTagControllerElement(ItemOrTagController control, YACLScreen screen, Dimension<Integer> dim) {
        super(control, screen, dim);
        this.itemOrTagController = control;
    }

    private boolean isSpecialTarget(String value) {
        return value.equals("*") || value.startsWith("!") || value.startsWith("regex:") || value.endsWith(":*") || value.startsWith("#");
    }

    private Component formatTargetText(String value) {
        if (value.equals("*")) return Component.literal(value).withStyle(ChatFormatting.AQUA);
        if (value.startsWith("!")) return Component.literal(value).withStyle(ChatFormatting.RED);
        if (value.startsWith("regex:")) return Component.literal(value).withStyle(ChatFormatting.GREEN);
        if (value.endsWith(":*")) return Component.literal(value).withStyle(ChatFormatting.YELLOW);
        if (value.startsWith("#")) return Component.literal(value).withStyle(ChatFormatting.GOLD);
        return Component.literal(value);
    }

    //? if yacl: <3.9.0 {
    @Override
    protected void drawValueText(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Dimension<Integer> oldDimension = this.getDimension();
        this.setDimension(this.getDimension().withWidth(oldDimension.width() - this.getDecorationPadding()));
        super.drawValueText(graphics, mouseX, mouseY, delta);
        this.setDimension(oldDimension);

        if (!this.currentItemIcon.isEmpty()) {
            graphics.renderFakeItem(this.currentItemIcon, this.getDimension().xLimit() - this.getXPadding() - this.getDecorationPadding() + 2, this.getDimension().y() + 2);
        }
    }
    //?} else {
    /*@Override
    protected void extractValueText(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Dimension<Integer> oldDimension = this.getDimension();
        this.setDimension(this.getDimension().withWidth(oldDimension.width() - this.getDecorationPadding()));
        super.extractValueText(graphics, mouseX, mouseY, delta);
        this.setDimension(oldDimension);

        if (!this.currentItemIcon.isEmpty()) {
            graphics.fakeItem(this.currentItemIcon, this.getDimension().xLimit() - this.getXPadding() - this.getDecorationPadding() + 2, this.getDimension().y() + 2);
        }
    }
    *///?}

    //? if yacl: <3.8.0 {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.itemOrTagController.option().available()) return false;

        if (button == 0) {
            if (this.isMouseOver(mouseX, mouseY)) {
                Dimension<Integer> oldDimension = this.getDimension();
                this.setDimension(this.getDimension().withWidth(oldDimension.width() - this.getDecorationPadding()));

                super.mouseClicked(mouseX, mouseY, button);
                this.setDimension(oldDimension);

                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    //?} elif yacl: <3.9.0 {
    /*@Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (!this.itemOrTagController.option().available()) return false;

        if (button == 0) {
            if (this.isMouseOver(mouseX, mouseY)) {
                Dimension<Integer> oldDimension = this.getDimension();
                this.setDimension(this.getDimension().withWidth(oldDimension.width() - this.getDecorationPadding()));

                super.onMouseClicked(mouseX, mouseY, button);
                this.setDimension(oldDimension);

                return true;
            }
        }
        return super.onMouseClicked(mouseX, mouseY, button);
    }
    *///?} else {
    /*@Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!this.itemOrTagController.option().available()) return false;

        if (event.button() == 0) {
            if (this.isMouseOver(event.x(), event.y())) {
                Dimension<Integer> oldDimension = this.getDimension();
                this.setDimension(this.getDimension().withWidth(oldDimension.width() - this.getDecorationPadding()));

                super.mouseClicked(event, doubleClick);
                this.setDimension(oldDimension);

                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
    *///?}

    @Override
    public void unfocus() {
        if (this.isDropdownVisible() && this.dropdownWidget() != null) {
            int index = this.dropdownWidget().selectedIndex();

            if (this.matchingValues != null && index >= 0 && index < this.matchingValues.size()) {
                this.inputField = this.matchingValues.get(index);
                this.caretPos = this.inputField.length();
                this.itemOrTagController.setFromString(this.inputField);
            }
            this.removeDropdownWidget();
        }
        super.unfocus();
    }

    //? if yacl: <3.8.0 {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.itemOrTagController.option().available()) return false;

        if (this.isDropdownVisible() && (keyCode == 257 || keyCode == 335)) {
            this.unfocus();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    //?} elif yacl: <3.9.0 {
    /*@Override
    public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.itemOrTagController.option().available()) return false;

        if (this.isDropdownVisible() && (keyCode == 257 || keyCode == 335)) {
            this.unfocus();
            return true;
        }
        return super.onKeyPressed(keyCode, scanCode, modifiers);
    }
    *///?} else {
    /*@Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!this.itemOrTagController.option().available()) return false;

        if (this.isDropdownVisible() && (event.key() == 257 || event.key() == 335)) {
            this.unfocus();
            return true;
        }
        return super.keyPressed(event);
    }
    *///?}

    @Override
    public List<String> computeMatchingValues() {
        List<String> identifiers = this.itemOrTagController.getAllowedValues(this.inputField).stream()
                .filter(s -> s.toLowerCase().contains(this.inputField.toLowerCase()))
                .collect(Collectors.toList());

        this.currentItemIcon = ItemStack.EMPTY;
        if (!isSpecialTarget(this.inputField)) {
            Item item = ItemRegistryHelper.getItem(this.inputField);
            if (item != Items.AIR) {
                this.currentItemIcon = new ItemStack(item);
            }
        }

        this.itemCache.clear();
        for (String id : identifiers) {
            if (!isSpecialTarget(id)) {
                Item item = ItemRegistryHelper.getItem(id);
                if (item != Items.AIR) {
                    this.itemCache.put(id, new ItemStack(item));
                }
            }
        }

        return identifiers;
    }

    //? if yacl: <3.9.0 {
    @Override
    protected void renderDropdownEntry(GuiGraphics graphics, Dimension<Integer> entryDimension, String value) {
        int leftEdge = entryDimension.x() + this.getDecorationPadding();
        
        Component text = formatTargetText(value);

        int maxTextWidth = entryDimension.width() - this.getDecorationPadding() - 24;
        
        if (this.textRenderer.width(text) > maxTextWidth) {
            String shortenedString = this.textRenderer.plainSubstrByWidth(text.getString(), maxTextWidth - this.textRenderer.width("...")) + "...";
            text = formatTargetText(shortenedString);
        }

        graphics.drawString(this.textRenderer, text, leftEdge + 4, this.getTextY(entryDimension), -1, true);

        ItemStack stack = this.itemCache.get(value);
        if (stack != null && !stack.isEmpty()) {
            graphics.renderFakeItem(stack, entryDimension.xLimit() - 20, entryDimension.y() + 1);
        }
    }
    //?} else {
    /*@Override
    protected void extractDropdownEntry(GuiGraphicsExtractor graphics, Dimension<Integer> entryDimension, String value) {
        int leftEdge = entryDimension.x() + this.getDecorationPadding();
        
        Component text = formatTargetText(value);

        int maxTextWidth = entryDimension.width() - this.getDecorationPadding() - 24;
        
        if (this.textRenderer.width(text) > maxTextWidth) {
            String shortenedString = this.textRenderer.plainSubstrByWidth(text.getString(), maxTextWidth - this.textRenderer.width("...")) + "...";
            text = formatTargetText(shortenedString);
        }

        graphics.text(this.textRenderer, text, leftEdge + 4, this.getTextY(entryDimension), -1, true);

        ItemStack stack = this.itemCache.get(value);
        if (stack != null && !stack.isEmpty()) {
            graphics.fakeItem(stack, entryDimension.xLimit() - 20, entryDimension.y() + 1);
        }
    }
    *///?}

    @Override
    public String getString(String value) {
        return value;
    }

    @Override
    protected Component getValueText() {
        if (!this.inputField.isEmpty() && !this.inputFieldFocused) {
            if (isSpecialTarget(this.inputField)) {
                return formatTargetText(this.inputField);
            }
            
            Item item = ItemRegistryHelper.getItem(this.inputField);
            if (item != Items.AIR) {
                return ItemRegistryHelper.getItemName(item);
            }
            
            return Component.literal(this.inputField);
        }
        return super.getValueText();
    }

    @Override
    protected int getDecorationPadding() { return 18; }
}
