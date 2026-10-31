package com.stalemated.lib.compat.yacl.registry;

import com.stalemated.lib.config.annotation.SharedDescription;
import com.stalemated.lib.config.model.OptionInfo;
import dev.isxander.yacl3.api.OptionDescription;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;

import static com.stalemated.lib.compat.yacl.registry.TranslationKeys.normalize;

/**
 * Strategy interface for resolving localized names, descriptions, and category/group titles for YACL.
 * <p>
 * Every generated key is {@code <prefix>.<family>.<path>}, where each segment of the path is converted to snake_case
 * and the dots between segments are kept: option {@code visualSettings.displayScale} becomes
 * {@code <prefix>.option.visual_settings.display_scale}.
 * Descriptions append {@code .desc} and show nothing when the key is missing.
 */
public interface YaclNamingStrategy {

    /**
     * Resolves the display title for a config option.
     *
     * @param prefix The translation prefix.
     * @param option The option metadata.
     * @return The localized name component.
     */
    default Component resolveName(String prefix, OptionInfo option) {
        return resolveName(prefix, option.getKey());
    }

    /**
     * Resolves the display title for an option key (such as a projected list element option).
     *
     * @param prefix The translation prefix.
     * @param optionKey The option key.
     * @return The localized name component.
     */
    default Component resolveName(String prefix, String optionKey) {
        return Component.translatable(resolveBaseKey(prefix, optionKey));
    }

    /**
     * Resolves the description or tooltip for a config option.
     *
     * @param prefix The translation prefix.
     * @param option The option metadata.
     * @param available Whether the option is currently interactively editable.
     * @return The option description.
     */
    default OptionDescription resolveDescription(String prefix, OptionInfo option, boolean available) {
        return describe(descriptionKey(prefix, option.getKey(), option.getField()), available);
    }

    /**
     * Resolves the description or tooltip for an option key (such as a projected list element option).
     *
     * @param prefix The translation prefix.
     * @param optionKey The option key.
     * @param available Whether the option is currently interactively editable.
     * @return The option description.
     */
    default OptionDescription resolveDescription(String prefix, String optionKey, boolean available) {
        return describe(descriptionKey(prefix, optionKey, null), available);
    }

    private String descriptionKey(String prefix, String optionKey, Field field) {
        SharedDescription shared = field != null ? field.getAnnotation(SharedDescription.class) : null;
        return shared != null
                ? prefix + ".description." + normalize(shared.value())
                : resolveBaseKey(prefix, optionKey) + ".desc";
    }

    private static OptionDescription describe(String descriptionKey, boolean available) {
        Component descText = Component.translatableWithFallback(descriptionKey, "");
        if (!available) {
            return OptionDescription.of(descText, Component.translatable(TranslationKeys.OP_REQUIRED));
        }
        return OptionDescription.of(descText);
    }

    /**
     * Resolves the translation base key for a config option.
     *
     * @param prefix The translation prefix.
     * @param option The option metadata.
     * @return The canonical base key string.
     */
    default String resolveBaseKey(String prefix, OptionInfo option) {
        return resolveBaseKey(prefix, option.getKey());
    }

    /**
     * Resolves the translation base key for an option key string.
     *
     * @param prefix The translation prefix.
     * @param optionKey The option key.
     * @return The canonical base key string.
     */
    default String resolveBaseKey(String prefix, String optionKey) {
        return prefix + ".option." + normalize(optionKey);
    }

    /**
     * Resolves the localized display component for an enum constant of a config option.
     * Uses {@code <baseKey>.<constant in snake_case>} with a fallback to {@code value.toString()}.
     *
     * @param prefix The translation prefix.
     * @param option The option metadata.
     * @param value The enum constant value.
     * @return The localized or fallback enum value component.
     */
    default Component resolveEnumValue(String prefix, OptionInfo option, Enum<?> value) {
        if (value == null) return Component.empty();
        String key = resolveBaseKey(prefix, option) + "." + normalize(value.name());
        return Component.translatableWithFallback(key, value.toString());
    }

    default Component resolveEmptyEntries() {
        return Component.translatable(TranslationKeys.NO_ENTRIES);
    }

    /**
     * Resolves the localized display component for a {@code @Title} section header or a label.
     * Titles share the group family, so a {@code @Title("x")} and a {@code group("x")} use the same key.
     *
     * @param prefix The translation prefix.
     * @param titleValue The raw value declared in {@code @Title}.
     * @return The localized title component.
     */
    default Component resolveTitle(String prefix, String titleValue) {
        return resolveGroupName(prefix, titleValue);
    }

    /**
     * Resolves the localized description component for a {@code @Title} section or group.
     *
     * @param prefix The translation prefix.
     * @param titleValue The raw value declared in {@code @Title}.
     * @return The localized title description component.
     */
    default Component resolveTitleDescription(String prefix, String titleValue) {
        return resolveGroupDescription(prefix, titleValue);
    }

    /**
     * Resolves the localized display title for a category tab.
     *
     * @param prefix The translation prefix.
     * @param categoryKey The category key or identifier.
     * @return The localized category title component.
     */
    default Component resolveCategoryName(String prefix, String categoryKey) {
        return Component.translatable(prefix + ".category." + normalize(categoryKey));
    }

    /**
     * Resolves the localized description for a category tab.
     *
     * @param prefix The translation prefix.
     * @param categoryKey The category key or identifier.
     * @return The localized category description component.
     */
    default Component resolveCategoryDescription(String prefix, String categoryKey) {
        return Component.translatableWithFallback(prefix + ".category." + normalize(categoryKey) + ".desc", "");
    }

    /**
     * Resolves the localized display title for an option group.
     *
     * @param prefix The translation prefix.
     * @param groupKey The group key or prefix.
     * @return The localized group title component.
     */
    default Component resolveGroupName(String prefix, String groupKey) {
        return Component.translatable(prefix + ".group." + normalize(groupKey));
    }

    /**
     * Resolves the localized description for an option group.
     *
     * @param prefix The translation prefix.
     * @param groupKey The group key or prefix.
     * @return The localized group description component.
     */
    default Component resolveGroupDescription(String prefix, String groupKey) {
        return Component.translatableWithFallback(prefix + ".group." + normalize(groupKey) + ".desc", "");
    }

    /**
     * Resolves the localized display title for a field within a nested POJO map entry.
     *
     * @param prefix The translation prefix.
     * @param mapInfo The parent map option metadata.
     * @param field The reflective field.
     * @return The localized POJO field title component.
     */
    default Component resolvePojoFieldName(String prefix, OptionInfo mapInfo, Field field) {
        return resolveName(prefix, mapInfo.getKey() + "." + field.getName());
    }

    /**
     * Resolves the localized description or tooltip for a field within a nested POJO map entry.
     *
     * @param prefix The translation prefix.
     * @param mapInfo The parent map option metadata.
     * @param field The reflective field.
     * @param available Whether the option is currently interactively editable.
     * @return The option description.
     */
    default OptionDescription resolvePojoFieldDescription(String prefix, OptionInfo mapInfo, Field field, boolean available) {
        return describe(descriptionKey(prefix, mapInfo.getKey() + "." + field.getName(), field), available);
    }

    YaclNamingStrategy DEFAULT = new YaclNamingStrategy() {};
}
