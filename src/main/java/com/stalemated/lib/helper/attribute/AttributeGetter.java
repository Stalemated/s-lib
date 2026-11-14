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

/**
 * Tooltip text for the weapon stats, enchantments, and food values of an {@link ItemStack}.
 * Decimal numbers have one decimal and use a dot ({@code "%.1f"} with {@link Locale#US}).
 * Weapon stats read the main-hand modifiers of the stack, not of a player.
 */
public final class AttributeGetter {

    private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0;

    private AttributeGetter() {}

    /**
     * Returns the main-hand attack damage of {@code stack}: the player base damage (1.0) with
     * the stack's attack damage modifiers applied.
     * <p>
     * Before 1.21 it also adds the enchantment damage bonus (Sharpness and similar). From 1.21
     * it does not, like the vanilla tooltip, because that damage depends on the target.
     * Attribute modifiers that come from enchantments still count.
     *
     * @param stack the item stack to inspect
     * @return the damage with one decimal, or {@code "0"} if the stack has no attack damage
     *         modifiers and no enchantment bonus
     */
    public static String calculateWeaponDamage(ItemStack stack) {
        AttributeTotal damage = mainHandTotal(stack, Attributes.ATTACK_DAMAGE);
        double enchantmentBonus = enchantmentDamageBonus(stack);
        if (damage.isEmpty() && enchantmentBonus == 0) return "0";
        return formatString(damage.valueFrom(PLAYER_BASE_ATTACK_DAMAGE) + enchantmentBonus);
    }

    /**
     * Returns the main-hand attack speed of {@code stack}: the player base attack speed (4.0)
     * with the stack's attack speed modifiers applied.
     *
     * @param stack the item stack to inspect
     * @return the attack speed with one decimal, or {@code "0"} if the stack has no attack
     *         speed modifiers
     */
    public static String calculateWeaponSpeed(ItemStack stack) {
        AttributeTotal speed = mainHandTotal(stack, Attributes.ATTACK_SPEED);
        if (speed.isEmpty()) return "0";
        return formatString(speed.valueFrom(PLAYER_BASE_ATTACK_SPEED));
    }

    //? if <1.20.5 {
    /**
     * Returns all enchantments on {@code stack} as newline-separated localized names, with
     * the level as vanilla shows it, for example {@code Sharpness V}.
     *
     * @param stack the item stack to inspect
     * @return newline-separated enchantment names, or {@code ""} if unenchanted
     */
    public static String getEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.isEmpty()) return "";

        List<String> formattedEnchants = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            formattedEnchants.add(entry.getKey().getFullname(entry.getValue()).getString());
        }

        return String.join("\n", formattedEnchants);
    }

    /**
     * Returns the food saturation value of {@code stack} formatted to one decimal place.
     *
     * @param stack the item stack to inspect
     * @return formatted saturation, or {@code ""} if {@code stack} is not food
     */
    public static String getSaturation(ItemStack stack) {
        FoodProperties food = stack.getItem().getFoodProperties();
        return food == null ? "" : formatString(food.getNutrition() * food.getSaturationModifier() * 2.0);
    }

    /**
     * Returns the food nutrition (hunger points) of {@code stack} as a string.
     *
     * @param stack the item stack to inspect
     * @return nutrition string, or {@code ""} if {@code stack} is not food
     */
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
    /*/^*
     * Returns all enchantments on {@code stack} as newline-separated localized names, with
     * the level as vanilla shows it, for example {@code Sharpness V}.
     *
     * @param stack the item stack to inspect
     * @return newline-separated enchantment names, or {@code ""} if unenchanted
     ^/
    public static String getEnchantments(ItemStack stack) {
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

    /^*
     * Returns the food saturation value of {@code stack} formatted to one decimal place.
     *
     * @param stack the item stack to inspect
     * @return formatted saturation, or {@code ""} if {@code stack} is not food
     ^/
    public static String getSaturation(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food == null ? "" : formatString(food.saturation());
    }

    /^*
     * Returns the food nutrition (hunger points) of {@code stack} as a string.
     *
     * @param stack the item stack to inspect
     * @return nutrition string, or {@code ""} if {@code stack} is not food
     ^/
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
