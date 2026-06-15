package com.stalemated.lib.helper.attribute;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;

import java.util.*;

public class AttributeGetter {
    public static String getEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
        if (enchantments.isEmpty()) return "";

        List<String> formattedEnchants = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            formattedEnchants.add(entry.getKey().getName(entry.getValue()).getString());
        }

        return String.join("\n", formattedEnchants);
    }

    public static String calculateWeaponDamage(ItemStack stack) {
        Collection<EntityAttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        double enchantDamage = EnchantmentHelper.getAttackDamage(stack, EntityGroup.DEFAULT);
        if (modifiers.isEmpty() && enchantDamage == 0) return "0";

        double damage = 1.0;
        for (EntityAttributeModifier modifier : modifiers) {
            damage += modifier.getValue();
        }
        damage += enchantDamage;
        return formatString((float) damage);
    }

    public static String calculateWeaponSpeed(ItemStack stack) {
        Collection<EntityAttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (modifiers.isEmpty()) return "0";

        double speed = 4.0;
        for (EntityAttributeModifier modifier : modifiers) {
            speed += modifier.getValue();
        }
        return formatString((float) speed);
    }

    public static String getSaturation(ItemStack stack) {
        if (stack.getItem().isFood()) {
            assert stack.getItem().getFoodComponent() != null;
            return formatString(stack.getItem().getFoodComponent().getSaturationModifier());
        }
        return "";
    }

    public static String getHunger(ItemStack stack) {
        if (stack.getItem().isFood()) {
            assert stack.getItem().getFoodComponent() != null;
            return String.valueOf(stack.getItem().getFoodComponent().getHunger());
        }
        return "";
    }

    private static String formatString(float unformatted) {
        return String.format(Locale.US, "%.1f", unformatted);
    }
}
