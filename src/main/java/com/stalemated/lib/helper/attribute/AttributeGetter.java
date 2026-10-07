package com.stalemated.lib.helper.attribute;

import java.util.*;

//? if <1.20.5 {
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
//?} else {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
*///?}

public class AttributeGetter {
    //? if <1.20.5 {
    public static String getEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.isEmpty()) return "";

        List<String> formattedEnchants = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            formattedEnchants.add(entry.getKey().getFullname(entry.getValue()).getString());
        }

        return String.join("\n", formattedEnchants);
    }

    public static String calculateWeaponDamage(ItemStack stack) {
        Collection<AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE);
        double enchantDamage = EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);
        if (modifiers.isEmpty() && enchantDamage == 0) return "0";

        double damage = 1.0;
        for (AttributeModifier modifier : modifiers) {
            damage += modifier.getAmount();
        }
        damage += enchantDamage;
        return formatString((float) damage);
    }

    public static String calculateWeaponSpeed(ItemStack stack) {
        Collection<AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_SPEED);
        if (modifiers.isEmpty()) return "0";

        double speed = 4.0;
        for (AttributeModifier modifier : modifiers) {
            speed += modifier.getAmount();
        }
        return formatString((float) speed);
    }

    public static String getSaturation(ItemStack stack) {
        if (stack.getItem().isEdible()) {
            assert stack.getItem().getFoodProperties() != null;
            return formatString(stack.getItem().getFoodProperties().getSaturationModifier());
        }
        return "";
    }

    public static String getHunger(ItemStack stack) {
        if (stack.getItem().isEdible()) {
            assert stack.getItem().getFoodProperties() != null;
            return String.valueOf(stack.getItem().getFoodProperties().getNutrition());
        }
        return "";
    }
    //?} else {
    /*public static String getEnchantments(ItemStack stack) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (enchantments.isEmpty()) return "";

        List<String> formattedEnchants = new ArrayList<>();
        for (var entry : enchantments.entrySet()) {
            //? if <1.21 {
            /^formattedEnchants.add(entry.getKey().value().getFullname(entry.getIntValue()).getString());
            ^///?} else {
            formattedEnchants.add(Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString());
            //?}
        }

        return String.join("\n", formattedEnchants);
    }

    public static String calculateWeaponDamage(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        double damage = 1.0;
        boolean hasModifiers = false;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
                damage += entry.modifier().amount();
                hasModifiers = true;
            }
        }
        if (!hasModifiers) return "0";
        return formatString((float) damage);
    }

    public static String calculateWeaponSpeed(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        double speed = 4.0;
        boolean hasModifiers = false;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_SPEED)) {
                speed += entry.modifier().amount();
                hasModifiers = true;
            }
        }
        if (!hasModifiers) return "0";
        return formatString((float) speed);
    }

    public static String getSaturation(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            return formatString(food.saturation());
        }
        return "";
    }

    public static String getHunger(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            return String.valueOf(food.nutrition());
        }
        return "";
    }
    *///?}

    private static String formatString(float unformatted) {
        return String.format(Locale.US, "%.1f", unformatted);
    }
}
