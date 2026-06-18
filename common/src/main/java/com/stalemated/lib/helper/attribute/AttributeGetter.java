package com.stalemated.lib.helper.attribute;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;

import java.util.*;

public class AttributeGetter {
    public static String getEnchantments(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.isEmpty()) return "";

        List<String> formattedEnchants = new ArrayList<>();
        for (var entry : enchantments.getEnchantmentEntries()) {
            formattedEnchants.add(Enchantment.getName(entry.getKey(), entry.getIntValue()).getString());
        }

        return String.join("\n", formattedEnchants);
    }

    public static String calculateWeaponDamage(ItemStack stack) {
        AttributeModifiersComponent modifiers = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);

        double damage = 1.0;
        boolean hasModifiers = false;
        for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
                damage += entry.modifier().value();
                hasModifiers = true;
            }
        }
        if (!hasModifiers) return "0";
        return formatString((float) damage);
    }

    public static String calculateWeaponSpeed(ItemStack stack) {
        AttributeModifiersComponent modifiers = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);

        double speed = 4.0;
        boolean hasModifiers = false;
        for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(EntityAttributes.GENERIC_ATTACK_SPEED)) {
                speed += entry.modifier().value();
                hasModifiers = true;
            }
        }
        if (!hasModifiers) return "0";
        return formatString((float) speed);
    }

    public static String getSaturation(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food != null) {
            return formatString(food.saturation());
        }
        return "";
    }

    public static String getHunger(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food != null) {
            return String.valueOf(food.nutrition());
        }
        return "";
    }

    private static String formatString(float unformatted) {
        return String.format(Locale.US, "%.1f", unformatted);
    }
}
