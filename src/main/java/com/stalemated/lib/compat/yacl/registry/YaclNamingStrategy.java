package com.stalemated.lib.compat.yacl.registry;

import net.minecraft.network.chat.Component;

import static com.stalemated.lib.compat.yacl.registry.TranslationKeys.normalize;

/**
 * Strategy interface for resolving localized names and descriptions of categories, groups, and options for YACL.
 * <p>
 * Every generated key is {@code <prefix>.<family>.<path>}, where each segment of the path is converted to snake_case
 * and the dots between segments are kept: option {@code visualSettings.displayScale} becomes
 * {@code <prefix>.option.visual_settings.display_scale}.
 * Descriptions append {@code .desc} and show nothing when the key is missing.
 * {@code @Title} sections use the group family, so a {@code @Title("x")} and a {@code group("x")} share their keys.
 */
public interface YaclNamingStrategy {

    /**
     * Resolves the localized display title for a category tab.
     *
     * @param prefix The translation prefix.
     * @param categoryKey The category key or identifier.
     * @return The localized category title component.
     */
    default Component categoryName(String prefix, String categoryKey) {
        return Component.translatable(prefix + ".category." + normalize(categoryKey));
    }

    /**
     * Resolves the localized description for a category tab.
     *
     * @param prefix The translation prefix.
     * @param categoryKey The category key or identifier.
     * @return The localized category description component.
     */
    default Component categoryDescription(String prefix, String categoryKey) {
        return Component.translatableWithFallback(prefix + ".category." + normalize(categoryKey) + ".desc", "");
    }

    /**
     * Resolves the localized display title for an option group or a {@code @Title} section.
     *
     * @param prefix The translation prefix.
     * @param groupKey The group key, nest prefix or {@code @Title} value.
     * @return The localized group title component.
     */
    default Component groupName(String prefix, String groupKey) {
        return Component.translatable(prefix + ".group." + normalize(groupKey));
    }

    /**
     * Resolves the localized description for an option group or a {@code @Title} section.
     *
     * @param prefix The translation prefix.
     * @param groupKey The group key, nest prefix or {@code @Title} value.
     * @return The localized group description component.
     */
    default Component groupDescription(String prefix, String groupKey) {
        return Component.translatableWithFallback(prefix + ".group." + normalize(groupKey) + ".desc", "");
    }

    /**
     * Resolves the display title for an option key, including fields of nested POJO entries
     * ({@code <container key>.<field name>}) and projected list element options.
     *
     * @param prefix The translation prefix.
     * @param optionKey The option key.
     * @return The localized name component.
     */
    default Component optionName(String prefix, String optionKey) {
        return Component.translatable(optionBaseKey(prefix, optionKey));
    }

    /**
     * Resolves the description or tooltip for an option key, including fields of nested POJO entries
     * and projected list element options.
     *
     * @param prefix The translation prefix.
     * @param optionKey The option key.
     * @return The option description.
     */
    default Component optionDescription(String prefix, String optionKey) {
        return Component.translatableWithFallback(optionBaseKey(prefix, optionKey) + ".desc", "");
    }

    default Component sharedDescription(String prefix, String descriptionKey) {
        return Component.translatableWithFallback(prefix + ".description." + normalize(descriptionKey), "");
    }

    /**
     * Resolves the localized display component for an enum constant of a config option.
     * Uses {@code <option key>.value.<constant in snake_case>} with a fallback to {@code value.toString()},
     * so a constant named {@code DESC} never shares the {@code .desc} key of the option.
     *
     * @param prefix The translation prefix.
     * @param optionKey The dot-separated key of the option.
     * @param value The enum constant value.
     * @return The localized or fallback enum value component.
     */
    default Component enumValueName(String prefix, String optionKey, Enum<?> value) {
        if (value == null) return Component.empty();
        String key = optionBaseKey(prefix, optionKey) + ".value." + normalize(value.name());
        return Component.translatableWithFallback(key, value.toString());
    }

    private String optionBaseKey(String prefix, String optionKey) {
        return prefix + ".option." + normalize(optionKey);
    }

    YaclNamingStrategy DEFAULT = new YaclNamingStrategy() {};
}
