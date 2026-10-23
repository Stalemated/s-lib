package com.stalemated.lib.compat.yacl.registry;

import com.stalemated.lib.config.model.OptionInfo;
import dev.isxander.yacl3.api.OptionDescription;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;

/**
 * Strategy interface for resolving localized names, descriptions, and category/group titles for YACL.
 */
@FunctionalInterface
public interface YaclNamingStrategy {

    /**
     * Resolves the display title for a config option.
     *
     * @param modId The mod identifier.
     * @param option The option metadata.
     * @return The localized name component.
     */
    Component resolveName(String modId, OptionInfo option);

    /**
     * Resolves the description or tooltip for a config option.
     *
     * @param modId The mod identifier.
     * @param option The option metadata.
     * @param available Whether the option is currently interactively editable.
     * @return The option description.
     */
    default OptionDescription resolveDescription(String modId, OptionInfo option, boolean available) {
        String baseKey = resolveBaseKey(modId, option);
        Component descText = Component.translatable(baseKey + ".desc");
        if (!available) {
            Component warnText = Component.translatable(modId + ".config_screen.op_required");
            return OptionDescription.of(descText, warnText);
        }
        return OptionDescription.of(descText);
    }

    /**
     * Resolves the translation base key for a config option.
     *
     * @param modId The mod identifier.
     * @param option The option metadata.
     * @return The canonical base key string.
     */
    default String resolveBaseKey(String modId, OptionInfo option) {
        return modId + ".option." + option.getKey().replace('.', '_');
    }

    /**
     * Resolves the localized display component for a {@code @Title} section header.
     * <p>
     * If {@code titleValue} already starts with {@code modId + "."} or {@code modId} is empty,
     * it is treated as a fully qualified translation key. Otherwise, it automatically prepends
     * {@code modId + ".title."} for concise declarations.
     *
     * @param modId The mod identifier.
     * @param titleValue The raw value declared in {@code @Title}.
     * @return The localized title component.
     */
    default Component resolveTitle(String modId, String titleValue) {
        if (titleValue == null || titleValue.isBlank()) {
            return Component.empty();
        }
        if (modId == null || modId.isEmpty() || titleValue.startsWith(modId + ".")) {
            return Component.translatable(titleValue);
        }
        return Component.translatable(modId + ".title." + titleValue);
    }

    /**
     * Resolves the localized description component for a {@code @Title} section or group.
     *
     * @param modId The mod identifier.
     * @param titleValue The raw value declared in {@code @Title}.
     * @return The localized title description component.
     */
    default Component resolveTitleDescription(String modId, String titleValue) {
        if (titleValue == null || titleValue.isBlank()) {
            return Component.empty();
        }
        if (modId == null || modId.isEmpty() || titleValue.startsWith(modId + ".")) {
            return Component.translatable(titleValue + ".desc");
        }
        return Component.translatable(modId + ".title." + titleValue + ".desc");
    }

    /**
     * Resolves the localized display title for a category tab.
     *
     * @param modId The mod identifier.
     * @param categoryKey The category key or identifier.
     * @return The localized category title component.
     */
    default Component resolveCategoryName(String modId, String categoryKey) {
        return Component.translatable(modId + ".category." + categoryKey);
    }

    /**
     * Resolves the localized tooltip for a category tab.
     *
     * @param modId The mod identifier.
     * @param categoryKey The category key or identifier.
     * @return The localized category tooltip component.
     */
    default Component resolveCategoryTooltip(String modId, String categoryKey) {
        return Component.translatable(modId + ".category." + categoryKey + ".desc");
    }

    /**
     * Resolves the localized display title for an option group.
     *
     * @param modId The mod identifier.
     * @param groupKey The group key or prefix.
     * @return The localized group title component.
     */
    default Component resolveGroupName(String modId, String groupKey) {
        return Component.translatable(modId + ".group." + groupKey.replace('.', '_'));
    }

    /**
     * Resolves the localized description for an option group.
     *
     * @param modId The mod identifier.
     * @param groupKey The group key or prefix.
     * @return The localized group description component.
     */
    default Component resolveGroupDescription(String modId, String groupKey) {
        return Component.translatable(modId + ".group." + groupKey.replace('.', '_') + ".desc");
    }

    /**
     * Resolves the localized display title for a field within a nested POJO map entry.
     *
     * @param modId The mod identifier.
     * @param mapInfo The parent map option metadata.
     * @param field The reflective field.
     * @return The localized POJO field title component.
     */
    default Component resolvePojoFieldName(String modId, OptionInfo mapInfo, Field field) {
        return Component.translatable(modId + ".option." + mapInfo.getKey() + "." + field.getName());
    }

    /**
     * Resolves the localized description or tooltip for a field within a nested POJO map entry.
     *
     * @param modId The mod identifier.
     * @param mapInfo The parent map option metadata.
     * @param field The reflective field.
     * @param available Whether the option is currently interactively editable.
     * @return The option description.
     */
    default OptionDescription resolvePojoFieldDescription(String modId, OptionInfo mapInfo, Field field, boolean available) {
        String baseKey = modId + ".option." + mapInfo.getKey() + "." + field.getName();
        Component descText = Component.translatable(baseKey + ".desc");
        if (!available) {
            Component warnText = Component.translatable(modId + ".config_screen.op_required");
            return OptionDescription.of(descText, warnText);
        }
        return OptionDescription.of(descText);
    }

    YaclNamingStrategy DEFAULT = new YaclNamingStrategy() {
        @Override
        public Component resolveName(String modId, OptionInfo option) {
            return Component.translatable(resolveBaseKey(modId, option));
        }
    };
}
