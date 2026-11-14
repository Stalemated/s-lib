package com.stalemated.lib.helper.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

//? if <1.20.5 {
import java.util.Map;
import net.minecraft.world.entity.MobType;
//?} else {
/*import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
*///?}

public final class AttributeGetter {

    private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0;

    private AttributeGetter() {}

    public static String calculateWeaponDamage(ItemStack stack) {
        AttributeTotal damage = mainHandTotal(stack, Attributes.ATTACK_DAMAGE);
        double enchantmentBonus = enchantmentDamageBonus(stack);
        if (damage.isEmpty() && enchantmentBonus == 0) return "0";
        return formatString(damage.valueFrom(PLAYER_BASE_ATTACK_DAMAGE) + enchantmentBonus);
    }

    public static String calculateWeaponSpeed(ItemStack stack) {
        AttributeTotal speed = mainHandTotal(stack, Attributes.ATTACK_SPEED);
        if (speed.isEmpty()) return "0";
        return formatString(speed.valueFrom(PLAYER_BASE_ATTACK_SPEED));
    }

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

    public static String getSaturation(ItemStack stack) {
        FoodProperties food = stack.getItem().getFoodProperties();
        return food == null ? "" : formatString(food.getNutrition() * food.getSaturationModifier() * 2.0);
    }

    public static String getHunger(ItemStack stack) {
        FoodProperties food = stack.getItem().getFoodProperties();
        return food == null ? "" : String.valueOf(food.getNutrition());
    }

    private static AttributeTotal mainHandTotal(ItemStack stack, Attribute attribute) {
        AttributeTotal total = new AttributeTotal();
        for (AttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute)) {
            switch (modifier.getOperation()) {
                case ADDITION -> total.add(modifier.getAmount());
                case MULTIPLY_BASE -> total.addMultipliedBase(modifier.getAmount());
                case MULTIPLY_TOTAL -> total.addMultipliedTotal(modifier.getAmount());
            }
        }
        return total;
    }

    private static double enchantmentDamageBonus(ItemStack stack) {
        return EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);
    }
    //?} else {
    /*public static String getEnchantments(ItemStack stack) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (enchantments.isEmpty()) return "";

        List<String> formattedEnchants = new ArrayList<>();
        for (var entry : enchantments.entrySet()) {
            //? if <1.21 {
            formattedEnchants.add(entry.getKey().value().getFullname(entry.getIntValue()).getString());
            //?} else {
            /^formattedEnchants.add(Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString());
            ^///?}
        }

        return String.join("\n", formattedEnchants);
    }

    public static String getSaturation(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food == null ? "" : formatString(food.saturation());
    }

    public static String getHunger(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food == null ? "" : String.valueOf(food.nutrition());
    }

    private static AttributeTotal mainHandTotal(ItemStack stack, Holder<Attribute> attribute) {
        AttributeTotal total = new AttributeTotal();
        stack.forEachModifier(EquipmentSlot.MAINHAND, (modified, modifier) -> {
            if (!modified.equals(attribute)) return;
            switch (modifier.operation()) {
                case ADD_VALUE -> total.add(modifier.amount());
                case ADD_MULTIPLIED_BASE -> total.addMultipliedBase(modifier.amount());
                case ADD_MULTIPLIED_TOTAL -> total.addMultipliedTotal(modifier.amount());
            }
        });
        return total;
    }

    private static double enchantmentDamageBonus(ItemStack stack) {
        //? if <1.21 {
        return EnchantmentHelper.getDamageBonus(stack, null);
        //?} else {
        /^return 0;
        ^///?}
    }
    *///?}

    private static String formatString(double unformatted) {
        return String.format(Locale.US, "%.1f", unformatted);
    }
}
