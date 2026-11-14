package com.stalemated.lib.compat.yacl.builder;

import com.stalemated.lib.compat.yacl.internal.OptionAssembler;
import com.stalemated.lib.compat.yacl.internal.ValueAccessor;
import com.stalemated.lib.compat.yacl.controller.builder.SelectDropdownControllerBuilder;
import com.stalemated.lib.compat.yacl.internal.YaclContext;
import com.stalemated.lib.compat.yacl.internal.TypeLens;
import com.stalemated.lib.compat.yacl.internal.YaclOptionMetadata;
import com.stalemated.lib.config.OptionInfo;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.controller.ValueFormatter;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/**
 * Fluent builder for a YACL {@link OptionGroup} inside a {@link YaclCategoryBuilder}.
 * Client only.
 * <p>
 * Most methods only record what to add. The options are created when the screen is built, in
 * the order of the calls, so a wrong key fails then and not when called.
 *
 * @param <T> the config model type
 */
@SuppressWarnings("UnusedReturnValue")
public class YaclGroupBuilder<T> {
    private final YaclContext<T> context;
    private Component name;
    private OptionDescription description;
    private boolean collapsed = false;
    private final List<Consumer<List<Option<?>>>> actions = new ArrayList<>();
    private final Map<String, Consumer<Option.Builder<?>>> overrides = new HashMap<>();
    private final Set<String> appliedOverrides = new HashSet<>();

    YaclGroupBuilder(YaclContext<T> context, Component defaultName) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.name = defaultName;
    }

    /**
     * Sets the name of this group. With an empty name, a {@code @Title} on the first option
     * becomes the group name.
     *
     * @param name the group name
     * @return this builder
     */
    public YaclGroupBuilder<T> name(Component name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the description of this group.
     *
     * @param description the description, or {@code null} for none
     * @return this builder
     */
    public YaclGroupBuilder<T> description(OptionDescription description) {
        this.description = description;
        return this;
    }

    /**
     * Same as {@link #description(OptionDescription)} with {@code OptionDescription.of(description)}.
     *
     * @param description the description text
     * @return this builder
     */
    public YaclGroupBuilder<T> description(Component description) {
        this.description = OptionDescription.of(description);
        return this;
    }

    /**
     * Sets whether this group starts collapsed. The default is {@code false}. When the screen
     * rebuilds, each group keeps the state the player left it in.
     *
     * @param collapsed {@code true} to start collapsed
     * @return this builder
     */
    public YaclGroupBuilder<T> collapsed(boolean collapsed) {
        this.collapsed = collapsed;
        return this;
    }

    /**
     * Adds a YACL option built elsewhere, at this position. YACL saves it through its own
     * binding; it is not part of the S-Lib draft.
     *
     * @param customOption the option to add
     * @return this builder
     */
    public YaclGroupBuilder<T> option(Option<?> customOption) {
        this.actions.add(list -> list.add(customOption));
        return this;
    }

    /**
     * Adds a {@link LabelOption} with {@code labelText} at this position.
     *
     * @param labelText the text to show
     * @return this builder
     */
    public YaclGroupBuilder<T> label(Component labelText) {
        this.actions.add(list -> list.add(LabelOption.create(labelText)));
        return this;
    }

    /**
     * Same as {@link #label(Component)} with the group name of {@code titleKey}, which is
     * {@code <prefix>.group.<titleKey>} by default. {@code titleKey} is always a translation key,
     * never literal text: {@code label("Combat Stats")} looks up {@code <prefix>.group.combat_stats}.
     *
     * @param titleKey key of the label text
     * @return this builder
     * @throws NullPointerException when called, if {@code titleKey} is {@code null}
     */
    public YaclGroupBuilder<T> label(String titleKey) {
        Component resolved = context.namingStrategy().groupName(context.translationPrefix(), titleKey);
        return label(resolved);
    }

    /**
     * Changes the YACL builder of the option with this key when the group adds it with
     * {@link #include(String...)}, {@link #includePrefix(String)} or
     * {@link #dropdown(String, List)}. It runs after S-Lib sets the name, description and
     * controller, so it can replace the controller; S-Lib sets the binding afterwards. The order
     * of this call and the include does not matter, and a second call for the same key replaces
     * the first.
     *
     * @param key exact key of the option
     * @param customizer changes the YACL builder
     * @return this builder
     * @throws IllegalArgumentException when the screen is built, if the group does not add the option with that key
     */
    public YaclGroupBuilder<T> configureOption(String key, Consumer<Option.Builder<?>> customizer) {
        this.overrides.put(key, customizer);
        return this;
    }

    /**
     * Same as {@link #dropdown(String, List, ValueFormatter)} with each value shown as it is.
     *
     * @param key key of an option edited as text, such as a {@code String} or an id
     * @param values the values to pick from; none can be {@code null}
     * @return this builder
     * @throws IllegalArgumentException when called, if no option has that key or the option is not edited as text
     */
    public YaclGroupBuilder<T> dropdown(String key, List<String> values) {
        return dropdown(key, values, Component::literal);
    }

    /**
     * Shows the option as a dropdown of {@code values} and adds it to this group. It is the same
     * as {@link #configureOption(String, Consumer)} with a {@link SelectDropdownControllerBuilder},
     * then {@link #include(String...)}.
     *
     * @param key key of an option edited as text, such as a {@code String} or an id
     * @param values the values to pick from; none can be {@code null}
     * @param formatter makes the text shown for each value
     * @return this builder
     * @throws IllegalArgumentException when called, if no option has that key or the option is not edited as text
     */
    @SuppressWarnings("unchecked")
    public YaclGroupBuilder<T> dropdown(String key, List<String> values, ValueFormatter<String> formatter) {
        requireEditedAsText(key);
        configureOption(key, b -> ((Option.Builder<String>) b).controller(opt ->
                SelectDropdownControllerBuilder.create(opt)
                        .values(values)
                        .formatValue(formatter)
        ));
        return include(key);
    }

    private void requireEditedAsText(String key) {
        OptionInfo info = context.session().getOptionTree().get(key);
        if (info == null) {
            throw new IllegalArgumentException("Unknown config option key: '" + key + "' in OptionGroup '" + groupName() + "'.");
        }
        boolean editedAsText = context.controllerRegistry().findLens(info.type())
                .map(lens -> lens.fallback() instanceof String)
                .orElse(false);
        if (!editedAsText) {
            throw new IllegalArgumentException("dropdown('" + key + "') needs an option edited as text, '" + key + "' is a " + info.type().getSimpleName());
        }
    }

    /**
     * Shows element {@code index} of a {@code List<E>} option as its own option in this group,
     * for example the two colors of a gradient. Its name and description come from
     * {@code optionKey}. If the list is shorter, setting the element fills the gap with
     * {@code defaultElement}. When the same list is also on the screen, both stay in sync.
     *
     * @param listKey key of a {@code List} option
     * @param index position of the element, from 0
     * @param optionKey key for the name and description, {@code <prefix>.option.<optionKey>} by default
     * @param defaultElement value used by Reset and to fill the list when it is shorter than {@code index + 1}; {@code null} uses the fallback of the element type, if that is a valid value
     * @param <E> the list element type
     * @return this builder
     * @throws IllegalArgumentException when the screen is built, if {@code listKey} is not a {@code List} option, or {@code defaultElement} is {@code null} and the fallback of the type is not a valid value
     * @throws UnsupportedOperationException when the screen is built, if the element type has no controller
     * @throws IndexOutOfBoundsException when the screen is built, if {@code index} is negative
     */
    public <E> YaclGroupBuilder<T> listElement(String listKey, int index, String optionKey, E defaultElement) {
        this.actions.add(list -> list.add(buildListElementOption(listKey, index, optionKey, defaultElement)));
        return this;
    }

    /**
     * Adds the simple options with these keys, in this order, each one with the controller of its
     * type. A {@code @Title} on an option adds a label before it; on the first option of a group
     * with no name, it names the group instead. {@code @EnabledIf} and
     * {@link #configureOption(String, Consumer)} apply too.
     * <p>
     * Lists and maps can't go inside a YACL group; add them with
     * {@link YaclCategoryBuilder#listOption(String)} or {@link YaclCategoryBuilder#mapPairsOption(String)}.
     *
     * @param keys the option keys
     * @return this builder
     * @throws NullPointerException when called, if {@code keys} is {@code null}
     * @throws IllegalArgumentException when the screen is built, if a key is unknown or names a list or map, or an annotation on the option is invalid
     * @throws UnsupportedOperationException when the screen is built, if an option has no controller
     */
    public YaclGroupBuilder<T> include(String... keys) {
        String[] keysCopy = keys.clone();
        actions.add(list -> {
            for (String key : keysCopy) {
                OptionInfo info = context.session().getOptionTree().get(key);
                if (info == null) {
                    throw new IllegalArgumentException("Unknown config option key: '" + key + "' in OptionGroup '" + groupName() + "'.");
                }
                if (info.isCollection() || info.isMap()) {
                    throw new IllegalArgumentException("Option '" + key + "' is a " + info.type().getSimpleName()
                            + " and cannot be included directly in an OptionGroup due to YACL hierarchy limits. "
                            + "Use category.listOption(\"" + key + "\") or category.mapPairsOption(\"" + key + "\") instead.");
                }
                appendOptionWithTitle(list, info);
            }
        });
        return this;
    }

    /**
     * Adds every simple option whose key is {@code prefix} or starts with it followed by a dot, in
     * the order of the config fields, as {@link #include(String...)} does. Lists and maps are
     * skipped.
     *
     * @param prefix key prefix, such as the key of a {@code @Nest} field; a trailing dot is fine
     * @return this builder
     * @throws IllegalArgumentException when the screen is built, if no option or {@code @Nest} category matches {@code prefix}
     * @throws UnsupportedOperationException when the screen is built, if an option has no controller
     */
    public YaclGroupBuilder<T> includePrefix(String prefix) {
        String cleanPrefix = prefix.endsWith(".") ? prefix : prefix + ".";
        actions.add(list -> {
            Collection<OptionInfo> options = context.session().getOptionTree().all();
            boolean matches = context.nestedCategory(prefix).isPresent()
                    || options.stream().anyMatch(info -> info.getKey().startsWith(cleanPrefix) || info.getKey().equals(prefix));
            if (!matches) {
                throw new IllegalArgumentException("No config option or nested category matches prefix '" + prefix + "' in OptionGroup '" + groupName() + "'");
            }
            for (OptionInfo info : options) {
                if (info.getKey().startsWith(cleanPrefix) || info.getKey().equals(prefix)) {
                    if (info.isCollection() || info.isMap()) {
                        continue;
                    }
                    appendOptionWithTitle(list, info);
                }
            }
        });
        return this;
    }

    private <E> Option<?> buildListElementOption(String listKey, int index, String optionKey, E defaultElement) {
        OptionInfo listInfo = context.session().getOptionTree().get(listKey);
        if (listInfo == null || !List.class.isAssignableFrom(listInfo.type())) {
            throw new IllegalArgumentException("Unknown or non-List option key for listElement: '" + listKey + "'");
        }

        Class<?> elemClass = defaultElement != null ? defaultElement.getClass() : listInfo.getCollectionElementType();
        YaclOptionMetadata meta = context.metadata(
                context.namingStrategy().optionName(context.translationPrefix(), optionKey),
                context.namingStrategy().optionDescription(context.translationPrefix(), optionKey),
                listInfo,
                optionKey
        );

        OptionInfo elementInfo = listInfo.childTemplate(listKey + "[" + index + "]", listInfo.field(), defaultElement, elemClass, elemClass);

        OptionAssembler assembler = context.assembler();
        TypeLens<E, Object> lens = assembler.<E, Object>lensFor(elemClass, meta).orElseThrow(() ->
                new UnsupportedOperationException("No YACL controller or adapter registered for list element type: "
                        + (elemClass != null ? elemClass.getName() : "null") + " on list option: " + listKey));

        E padding = defaultElement != null ? defaultElement : lens.fromGui().apply(lens.fallback());
        if (padding == null) {
            throw new IllegalArgumentException("listElement '" + optionKey + "' of list '" + listKey + "' needs a default element, since '"
                    + lens.fallback() + "' is not a valid " + elemClass.getSimpleName());
        }
        return assembler.option(meta, elementInfo, ValueAccessor.listElement(context.session(), listInfo, index, padding), lens, defaultElement, null);
    }

    private String groupName() {
        return name != null ? name.getString() : "unnamed";
    }

    private boolean hasGroupTitle() {
        return name != null && !name.equals(Component.empty()) && !name.getString().isEmpty();
    }

    private void appendOptionWithTitle(List<Option<?>> list, OptionInfo info) {
        if (info.hasTitle()) {
            appendTitle(list, info.getTitle());
        }
        Consumer<Option.Builder<?>> override = overrides.get(info.getKey());
        if (override != null) {
            appliedOverrides.add(info.getKey());
        }
        list.add(context.assembler().option(context.resolveMetadata(info), info, override));
    }

    private void appendTitle(List<Option<?>> list, String title) {
        Component titleText = context.namingStrategy().groupName(context.translationPrefix(), title);
        if (!list.isEmpty()) {
            list.add(LabelOption.create(titleText));
        } else if (!hasGroupTitle()) {
            this.name = titleText;
            if (description == null) {
                this.description = OptionDescription.of(context.namingStrategy().groupDescription(context.translationPrefix(), title));
            }
        } else if (!titleText.equals(name)) {
            list.add(LabelOption.create(titleText));
        }
    }

    OptionGroup build() {
        return toGroup(resolveOptions());
    }

    Optional<OptionGroup> buildUnlessEmpty() {
        List<Option<?>> resolvedOptions = resolveOptions();
        return resolvedOptions.isEmpty() ? Optional.empty() : Optional.of(toGroup(resolvedOptions));
    }

    private List<Option<?>> resolveOptions() {
        appliedOverrides.clear();
        List<Option<?>> resolvedOptions = new ArrayList<>();
        for (Consumer<List<Option<?>>> action : actions) {
            action.accept(resolvedOptions);
        }
        for (String key : overrides.keySet()) {
            if (!appliedOverrides.contains(key)) {
                throw new IllegalArgumentException("configureOption('" + key + "') in OptionGroup '" + groupName() + "' targets an option the group does not include");
            }
        }
        return resolvedOptions;
    }

    private OptionGroup toGroup(List<Option<?>> resolvedOptions) {
        OptionGroup.Builder builder = OptionGroup.createBuilder()
                .name(name)
                .collapsed(collapsed)
                .options(resolvedOptions);

        if (description != null) {
            builder.description(description);
        }
        return builder.build();
    }
}
