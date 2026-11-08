package com.stalemated.lib.compat.yacl.internal;

import com.mojang.blaze3d.platform.InputConstants;
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
import com.stalemated.lib.predicate.target.TargetKind;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ItemOrTagControllerElement extends AbstractDropdownControllerElement<String, String> {
    private static final Map<TargetKind, ChatFormatting> COLORS_BY_KIND = new EnumMap<>(Map.of(
            TargetKind.ALL, ChatFormatting.AQUA,
            TargetKind.NEGATED, ChatFormatting.RED,
            TargetKind.REGEX, ChatFormatting.GREEN,
            TargetKind.NAMESPACE, ChatFormatting.YELLOW,
            TargetKind.TAG, ChatFormatting.GOLD
    ));

    private final ItemOrTagController itemOrTagController;
    private final Map<String, ItemStack> iconsByTarget = new HashMap<>();
    private ItemStack currentItemIcon = ItemStack.EMPTY;

    public ItemOrTagControllerElement(ItemOrTagController control, YACLScreen screen, Dimension<Integer> dim) {
        super(control, screen, dim);
        this.itemOrTagController = control;
    }

    private static boolean isEnterKey(int keyCode) {
        return keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER;
    }

    private static boolean isSpecialTarget(String value) {
        return TargetKind.of(value) != TargetKind.ITEM;
    }

    private static ItemStack iconFor(String target) {
        if (isSpecialTarget(target)) return ItemStack.EMPTY;
        return ItemRegistryHelper.getIcon(ItemRegistryHelper.getItem(target));
    }

    private ItemStack cachedIconFor(String target) {
        return iconsByTarget.computeIfAbsent(target, ItemOrTagControllerElement::iconFor);
    }

    private Component formatTargetText(String value) {
        ChatFormatting color = COLORS_BY_KIND.get(TargetKind.of(value));
        return color != null ? Component.literal(value).withStyle(color) : Component.literal(value);
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

    //? if >=26.3 {
    /*private final PopupTextFocus popupTextFocus = new PopupTextFocus();

    @Override
    public void setFocused(boolean focused) {
        if (popupTextFocus.opening()) return;
        super.setFocused(focused);
    }
    *///?}

    //? if yacl: <3.8.0 {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.itemOrTagController.option().available()) return false;

        if (this.isDropdownVisible() && isEnterKey(keyCode)) {
            this.unfocus();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    //?} elif yacl: <3.9.0 {
    /*@Override
    public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.itemOrTagController.option().available()) return false;

        if (this.isDropdownVisible() && isEnterKey(keyCode)) {
            this.unfocus();
            return true;
        }
        return super.onKeyPressed(keyCode, scanCode, modifiers);
    }
    *///?} else {
    /*@Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!this.itemOrTagController.option().available()) return false;

        if (this.isDropdownVisible() && isEnterKey(event.key())) {
            this.unfocus();
            return true;
        }
        return super.keyPressed(event);
    }
    *///?}

    @Override
    public List<String> computeMatchingValues() {
        if (this.iconsByTarget.size() > ItemTargetIndex.MAX_MATCHES) {
            this.iconsByTarget.clear();
        }
        this.currentItemIcon = cachedIconFor(this.inputField);
        if (!this.inputFieldFocused) return List.of();
        return this.itemOrTagController.targets().matching(this.inputField);
    }

    @Override
    public void createDropdownWidget() {
        this.matchingValues = computeMatchingValues();
        //? if >=26.3 {
        /*popupTextFocus.open(this, screen, super::createDropdownWidget);
        *///?} else {
        super.createDropdownWidget();
        //?}
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

        ItemStack stack = cachedIconFor(value);
        if (!stack.isEmpty()) {
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

        ItemStack stack = cachedIconFor(value);
        if (!stack.isEmpty()) {
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
