package com.stalemated.lib.compat.yacl.controller;

import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.dropdown.AbstractDropdownControllerElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

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
        return value.equals("*") || value.startsWith("regex:") || value.endsWith(":*") || value.startsWith("#");
    }

    private Text formatTargetText(String value) {
        if (value.equals("*")) return Text.literal(value).formatted(Formatting.AQUA);
        if (value.startsWith("regex:")) return Text.literal(value).formatted(Formatting.GREEN);
        if (value.endsWith(":*")) return Text.literal(value).formatted(Formatting.YELLOW);
        if (value.startsWith("#")) return Text.literal(value).formatted(Formatting.GOLD);
        return Text.literal(value);
    }

    @Override
    protected void drawValueText(DrawContext graphics, int mouseX, int mouseY, float delta) {
        Dimension<Integer> oldDimension = this.getDimension();
        this.setDimension(this.getDimension().withWidth(oldDimension.width() - this.getDecorationPadding()));
        super.drawValueText(graphics, mouseX, mouseY, delta);
        this.setDimension(oldDimension);

        if (!this.currentItemIcon.isEmpty()) {
            graphics.drawItemWithoutEntity(this.currentItemIcon, this.getDimension().xLimit() - this.getXPadding() - this.getDecorationPadding() + 2, this.getDimension().y() + 2);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.isDropdownVisible() && (keyCode == 257 || keyCode == 335)) {
            this.unfocus();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public List<String> computeMatchingValues() {
        List<String> identifiers = this.itemOrTagController.getAllowedValues(this.inputField).stream()
                .filter(s -> s.toLowerCase().contains(this.inputField.toLowerCase()))
                .collect(Collectors.toList());

        this.currentItemIcon = ItemStack.EMPTY;
        if (!isSpecialTarget(this.inputField)) {
            try {
                Item item = Registries.ITEM.get(Identifier.of(this.inputField));
                if (item != Items.AIR) {
                    this.currentItemIcon = new ItemStack(item);
                }
            } catch (Exception ignored) {}
        }

        this.itemCache.clear();
        for (String id : identifiers) {
            if (!isSpecialTarget(id)) {
                try {
                    Item item = Registries.ITEM.get(Identifier.of(id));
                    if (item != Items.AIR) {
                        this.itemCache.put(id, new ItemStack(item));
                    }
                } catch (Exception ignored) {}
            }
        }

        return identifiers;
    }

    @Override
    protected void renderDropdownEntry(DrawContext graphics, Dimension<Integer> entryDimension, String value) {
        int leftEdge = entryDimension.x() + this.getDecorationPadding();

        Text text = formatTargetText(value);

        int maxTextWidth = entryDimension.width() - this.getDecorationPadding() - 24;

        if (this.textRenderer.getWidth(text) > maxTextWidth) {
            String shortenedString = this.textRenderer.trimToWidth(text.getString(), maxTextWidth - this.textRenderer.getWidth("...")) + "...";
            text = formatTargetText(shortenedString);
        }

        graphics.drawText(this.textRenderer, text, leftEdge + 4, this.getTextY(entryDimension), -1, true);

        ItemStack stack = this.itemCache.get(value);
        if (stack != null && !stack.isEmpty()) {
            graphics.drawItemWithoutEntity(stack, entryDimension.xLimit() - 20, entryDimension.y() + 1);
        }
    }

    @Override
    public String getString(String value) {
        return value;
    }

    @Override
    protected Text getValueText() {
        if (!this.inputField.isEmpty() && !this.inputFieldFocused) {
            if (isSpecialTarget(this.inputField)) {
                return formatTargetText(this.inputField);
            }

            try {
                Item item = Registries.ITEM.get(Identifier.of(this.inputField));
                if (item != Items.AIR) {
                    return item.getName();
                }
            } catch (Exception ignored) {}

            return Text.literal(this.inputField);
        }
        return super.getValueText();
    }

    @Override
    protected int getDecorationPadding() { return 18; }
}