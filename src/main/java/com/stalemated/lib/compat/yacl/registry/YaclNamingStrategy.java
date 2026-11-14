package com.stalemated.lib.compat.yacl.registry;

import net.minecraft.network.chat.Component;

import static com.stalemated.lib.compat.yacl.registry.TranslationKeys.normalize;

/**
 * Makes the names and descriptions shown on a YACL screen. Override only the methods you need;
 * all of them have a default. Client only.
 * <p>
 * Every default key is {@code <prefix>.<family>.<path>}, where each segment of the path is
 * converted to snake_case and the dots between segments are kept: option
 * {@code visualSettings.displayScale} becomes {@code <prefix>.option.visual_settings.display_scale}.
 * Acronyms are split, so {@code HTTPServer} gives {@code http_server}, and other characters
 * become {@code _}. Descriptions append {@code .desc} and show nothing when the key is missing.
 * {@code @Title} sections use the group family, so a {@code @Title("x")} and a
 * {@code group("x")} share their keys.
 */
public interface YaclNamingStrategy {

    /**
     * Returns the name of a tab: {@code <prefix>.category.<categoryKey>}, with no fallback.
     *
     * @param prefix the screen translation prefix
     * @param categoryKey the {@code tabKey} given to {@code category}
     * @return the tab name
     * @throws NullPointerException if {@code categoryKey} is {@code null}
     */
    default Component categoryName(String prefix, String categoryKey) {
        return Component.translatable(prefix + ".category." + normalize(categoryKey));
    }

    /**
     * Returns the tooltip of a tab: {@code <prefix>.category.<categoryKey>.desc}, empty when the
     * key is missing.
     *
     * @param prefix the screen translation prefix
     * @param categoryKey the {@code tabKey} given to {@code category}
     * @return the tab tooltip
     * @throws NullPointerException if {@code categoryKey} is {@code null}
     */
    default Component categoryDescription(String prefix, String categoryKey) {
        return Component.translatableWithFallback(prefix + ".category." + normalize(categoryKey) + ".desc", "");
    }

    /**
     * Returns the name of a group, {@code @Nest} section, {@code label(String)} text or
     * {@code @Title}: {@code <prefix>.group.<groupKey>}, with no fallback.
     *
     * @param prefix the screen translation prefix
     * @param groupKey the group key, nest prefix or {@code @Title} value
     * @return the group name
     * @throws NullPointerException if {@code groupKey} is {@code null}
     */
    default Component groupName(String prefix, String groupKey) {
        return Component.translatable(prefix + ".group." + normalize(groupKey));
    }

    /**
     * Returns the description of a group, {@code @Nest} section or {@code @Title}:
     * {@code <prefix>.group.<groupKey>.desc}, empty when the key is missing.
     *
     * @param prefix the screen translation prefix
     * @param groupKey the group key, nest prefix or {@code @Title} value
     * @return the group description
     * @throws NullPointerException if {@code groupKey} is {@code null}
     */
    default Component groupDescription(String prefix, String groupKey) {
        return Component.translatableWithFallback(prefix + ".group." + normalize(groupKey) + ".desc", "");
    }

    /**
     * Returns the name of an option: {@code <prefix>.option.<optionKey>}, with no fallback. POJO
     * fields use {@code <container key>.<field name>} as key, and {@code listElement} uses its
     * {@code optionKey}.
     * <p>
     * Two options of the same config must not get the same name: building the screen fails with
     * an {@code IllegalStateException}.
     *
     * @param prefix the screen translation prefix
     * @param optionKey the dot-separated option key
     * @return the option name
     * @throws NullPointerException if {@code optionKey} is {@code null}
     */
    default Component optionName(String prefix, String optionKey) {
        return Component.translatable(optionBaseKey(prefix, optionKey));
    }

    /**
     * Returns the description of an option: {@code <prefix>.option.<optionKey>.desc}, empty when
     * the key is missing. A field with {@code @SharedDescription} uses
     * {@link #sharedDescription(String, String)} instead.
     *
     * @param prefix the screen translation prefix
     * @param optionKey the dot-separated option key
     * @return the option description
     * @throws NullPointerException if {@code optionKey} is {@code null}
     */
    default Component optionDescription(String prefix, String optionKey) {
        return Component.translatableWithFallback(optionBaseKey(prefix, optionKey) + ".desc", "");
    }

    /**
     * Returns the description named by a {@code @SharedDescription}:
     * {@code <prefix>.description.<descriptionKey>}, empty when the key is missing.
     *
     * @param prefix the screen translation prefix
     * @param descriptionKey the value of the {@code @SharedDescription}
     * @return the shared description
     * @throws NullPointerException if {@code descriptionKey} is {@code null}
     */
    default Component sharedDescription(String prefix, String descriptionKey) {
        return Component.translatableWithFallback(prefix + ".description." + normalize(descriptionKey), "");
    }

    /**
     * Returns the name of an enum constant: {@code <prefix>.option.<optionKey>.value.<constant>},
     * with the constant in snake_case and {@code value.toString()} as fallback. The
     * {@code .value.} part keeps a constant named {@code DESC} from sharing the {@code .desc} key
     * of the option.
     *
     * @param prefix the screen translation prefix
     * @param optionKey the dot-separated option key
     * @param value the enum constant, or {@code null}
     * @return the constant name, or {@link Component#empty()} if {@code value} is {@code null}
     */
    default Component enumValueName(String prefix, String optionKey, Enum<?> value) {
        if (value == null) return Component.empty();
        String key = optionBaseKey(prefix, optionKey) + ".value." + normalize(value.name());
        return Component.translatableWithFallback(key, value.toString());
    }

    private String optionBaseKey(String prefix, String optionKey) {
        return prefix + ".option." + normalize(optionKey);
    }

    /** Default naming strategy using the standard {@code <prefix>.<family>.<path>} keys. */
    YaclNamingStrategy DEFAULT = new YaclNamingStrategy() {};
}
