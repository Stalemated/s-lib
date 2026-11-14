package com.stalemated.lib.compat.yacl.builder;

import com.google.common.collect.ImmutableList;
import com.stalemated.lib.compat.yacl.internal.ValueAccessor;
import com.stalemated.lib.compat.yacl.internal.YaclContext;
import com.stalemated.lib.compat.yacl.internal.YaclMapOptionFactory;
import com.stalemated.lib.compat.yacl.internal.YaclOptionMetadata;
import com.stalemated.lib.compat.yacl.internal.YaclPojoFieldBuilder;
import com.stalemated.lib.config.annotation.Title;
import com.stalemated.lib.config.internal.CategoryNode;
import com.stalemated.lib.config.internal.ConfigNode;
import com.stalemated.lib.config.OptionInfo;
import com.stalemated.lib.config.OptionTree;
import com.stalemated.lib.util.internal.ReflectionUtils;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

import static com.stalemated.lib.SLib.LOGGER;

/**
 * Fluent builder for a YACL {@link ConfigCategory} tab within a {@link YaclScreenBuilder}.
 * Client only.
 * <p>
 * The screen makes a new builder every time it is built, and groups show up in the order of the
 * calls. The methods run while the screen is built, so their errors show up there.
 *
 * @param <T> the config model type
 */
@SuppressWarnings("UnusedReturnValue")
public class YaclCategoryBuilder<T> {
    private static final String NO_ENTRIES = "s_lib.config_screen.no_entries";
    private static final String REMOVE_ENTRY = "s_lib.config_screen.remove_entry";
    private static final String ADD_ENTRY = "s_lib.config_screen.add_entry";
    private static final String ADD_MAP_ENTRY_DESC = "s_lib.config_screen.add_map_entry.desc";
    private static final String NEW_KEY = "s_lib.config_screen.new_key";

    private final YaclContext<T> context;
    private final YaclMapOptionFactory<T> mapOptionFactory;

    private Component name;
    private Component description;
    private final List<OptionGroup> groups = new ArrayList<>();

    YaclCategoryBuilder(YaclContext<T> context) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.mapOptionFactory = new YaclMapOptionFactory<>(context);
        this.name = Component.literal(context.session().getManager().getName());
    }

    /**
     * Sets the name of this tab. By default it comes from the naming strategy:
     * {@code <prefix>.category.<tabKey>}.
     *
     * @param name the tab name
     * @return this builder
     */
    public YaclCategoryBuilder<T> name(Component name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the tooltip of this tab. By default it comes from the naming strategy:
     * {@code <prefix>.category.<tabKey>.desc}, empty when that key is missing.
     *
     * @param description the tooltip, or {@code null} for none
     * @return this builder
     */
    public YaclCategoryBuilder<T> description(Component description) {
        this.description = description;
        return this;
    }

    /**
     * Adds a YACL group built elsewhere. YACL saves its options through their own bindings; they
     * are not part of the S-Lib draft.
     *
     * @param customGroup the group to add
     * @return this builder
     */
    public YaclCategoryBuilder<T> group(OptionGroup customGroup) {
        this.groups.add(customGroup);
        return this;
    }

    /**
     * Adds a group and lets {@code consumer} fill it. By default the group is named
     * {@code <prefix>.group.<groupKey>}, with the description {@code <prefix>.group.<groupKey>.desc};
     * a {@code @Title} with the same value uses the same keys. The group starts expanded.
     *
     * @param groupKey key of the group
     * @param consumer fills the group
     * @return this builder
     * @throws NullPointerException if {@code consumer} is {@code null}
     * @throws IllegalArgumentException if the group can't be built, for example because of an unknown option key
     * @throws UnsupportedOperationException if an option of the group has no controller
     */
    public YaclCategoryBuilder<T> group(String groupKey, Consumer<YaclGroupBuilder<T>> consumer) {
        Component groupName = context.namingStrategy().groupName(context.translationPrefix(), groupKey);
        YaclGroupBuilder<T> builder = new YaclGroupBuilder<>(context, groupName);
        builder.description(context.namingStrategy().groupDescription(context.translationPrefix(), groupKey));
        consumer.accept(builder);
        groups.add(builder.build());
        return this;
    }

    /**
     * Adds a section header: a group with no collapse bar that only shows {@code labelText}.
     *
     * @param labelText the text to show
     * @return this builder
     * @throws NullPointerException if {@code labelText} is {@code null}
     */
    public YaclCategoryBuilder<T> label(Component labelText) {
        groups.add(YaclSectionHeaderGroup.of(labelText));
        return this;
    }

    /**
     * Same as {@link #label(Component)} with the group name of {@code titleKey}, which is
     * {@code <prefix>.group.<titleKey>} by default. {@code titleKey} is always a translation key,
     * never literal text: {@code label("Combat Stats")} looks up {@code <prefix>.group.combat_stats}.
     *
     * @param titleKey key of the header text
     * @return this builder
     * @throws NullPointerException if {@code titleKey} is {@code null}
     */
    public YaclCategoryBuilder<T> label(String titleKey) {
        return label(context.namingStrategy().groupName(context.translationPrefix(), titleKey));
    }

    /**
     * Same as {@link #listOption(String, Consumer)} with no customizer.
     *
     * @param key key of a {@code Collection} option
     * @return this builder
     * @throws IllegalArgumentException if no option has that key, or its {@code @EnabledIf} is invalid
     * @throws UnsupportedOperationException if the element type has no controller or no initial value
     */
    public YaclCategoryBuilder<T> listOption(String key) {
        return listOption(key, null);
    }

    /**
     * Adds a YACL {@link ListOption} for a collection of simple values, each one edited with the
     * controller of its type. A {@code Set} is edited as a list and saved as a set. If the field
     * has a {@code @Title}, a header goes first, and {@code @EnabledIf} applies too. For lists of
     * POJOs, use {@link #nestedList(String)}.
     *
     * @param key key of a {@code Collection} option
     * @param customizer changes the YACL builder after S-Lib sets the name, description, controller and initial element, or {@code null}
     * @return this builder
     * @throws IllegalArgumentException if no option has that key, or its {@code @EnabledIf} is invalid
     * @throws UnsupportedOperationException if the element type has no controller or no initial value
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public YaclCategoryBuilder<T> listOption(String key, Consumer<ListOption.Builder<?>> customizer) {
        OptionInfo info = requireOptionInfo(key);
        if (info.hasTitle()) {
            label(info.getTitle());
        }
        ListOption<?> listOpt = context.assembler().list(context.resolveMetadata(info), info, (Consumer) customizer);
        groups.add(listOpt);
        return this;
    }

    /**
     * Same as {@link #mapPairsOption(String, Consumer)} with no customizer.
     *
     * @param key key of a {@code Map} option with {@code String} keys
     * @return this builder
     * @throws IllegalArgumentException if no option has that key, the map keys are not {@code String}, or its {@code @EnabledIf} is invalid
     * @throws UnsupportedOperationException if the map values can't be converted from text
     */
    public YaclCategoryBuilder<T> mapPairsOption(String key) {
        return mapPairsOption(key, null);
    }

    /**
     * Adds a {@link ListOption} that edits a {@code Map<String, V>} option as {@code key: value}
     * lines. Keys with {@code :}, spaces or quotes are quoted. When saving, a line that can't be
     * read, or whose value can't be converted, is skipped with a warning. A {@code SortedMap} is
     * saved as a {@code TreeMap}. {@code @Title} and {@code @EnabledIf} apply as in
     * {@link #listOption(String, Consumer)}.
     *
     * @param key key of a {@code Map} option with {@code String} keys
     * @param customizer changes the YACL builder after S-Lib sets the name, description, controller and initial element, or {@code null}
     * @return this builder
     * @throws IllegalArgumentException if no option has that key, the map keys are not {@code String}, or its {@code @EnabledIf} is invalid
     * @throws UnsupportedOperationException if the map values can't be converted from text
     */
    public YaclCategoryBuilder<T> mapPairsOption(String key, Consumer<ListOption.Builder<String>> customizer) {
        OptionInfo info = requireOptionInfo(key);
        if (info.hasTitle()) {
            label(info.getTitle());
        }
        ListOption<String> mapOpt = mapOptionFactory.buildMapPairsOption(info, customizer);
        groups.add(mapOpt);
        return this;
    }

    /**
     * Same as {@link #nested(String, Consumer)} with no customizer.
     *
     * @param nestPrefix key of a {@code @Nest} field
     * @return this builder
     * @throws IllegalArgumentException if {@code nestPrefix} is not a {@code @Nest} category
     * @throws UnsupportedOperationException if an option under it has no controller
     */
    public YaclCategoryBuilder<T> nested(String nestPrefix) {
        return nested(nestPrefix, null);
    }

    /**
     * Adds an expanded group with every simple option under {@code nestPrefix}, including the
     * options of the nests inside it. The group is named like in {@link #group(String, Consumer)},
     * with the {@code @Title} of the {@code @Nest} field as key, or {@code nestPrefix} if it has
     * none. Then each list or map under the prefix is added as a group of its own. If the nest has
     * only lists and maps, the first group is left out.
     *
     * @param nestPrefix key of a {@code @Nest} field; a trailing dot is fine
     * @param customizer changes the group before it is built, or {@code null}
     * @return this builder
     * @throws IllegalArgumentException if {@code nestPrefix} is not a {@code @Nest} category
     * @throws UnsupportedOperationException if an option under it has no controller
     */
    public YaclCategoryBuilder<T> nested(String nestPrefix, Consumer<YaclGroupBuilder<T>> customizer) {
        CategoryNode catNode = context.nestedCategory(nestPrefix)
                .orElseThrow(() -> new IllegalArgumentException("Unknown nested category key: '" + nestPrefix + "'"));
        String groupKey = catNode.hasTitle() ? catNode.title() : nestPrefix;
        YaclGroupBuilder<T> builder = new YaclGroupBuilder<>(context, context.namingStrategy().groupName(context.translationPrefix(), groupKey));
        builder.collapsed(false);
        builder.description(context.namingStrategy().groupDescription(context.translationPrefix(), groupKey));
        builder.includePrefix(nestPrefix);
        if (customizer != null) {
            customizer.accept(builder);
        }
        builder.buildUnlessEmpty().ifPresent(groups::add);

        // Emit any Collection or Map options nested under this prefix as sibling YACL groups
        String cleanPrefix = nestPrefix.endsWith(".") ? nestPrefix : nestPrefix + ".";
        for (OptionInfo info : context.session().getOptionTree().all()) {
            if (info.getKey().startsWith(cleanPrefix) && (info.isCollection() || info.isMap())) {
                emitCollectionOrMapOption(info);
            }
        }
        return this;
    }

    /**
     * Adds the entries of a map of POJOs. A header holds a New Entry Key field and an Add Entry
     * button. Then each entry gets a collapsed group, named after its key, with one option per
     * POJO field and a Remove Entry button. Add Entry ignores a blank or repeated key and logs
     * why. The changes stay in the draft until the player saves.
     * <p>
     * The Add Entry and Remove Entry buttons need the screen from
     * {@link YaclScreenBuilder#generateScreen()}.
     *
     * @param mapKey key of a {@code Map} option with {@code String} keys
     * @return this builder
     * @throws IllegalArgumentException if no option has that key, or the map keys are not {@code String}
     * @throws UnsupportedOperationException if the entry type has no no-args constructor or initial supplier, or a field has no controller
     */
    @SuppressWarnings({"unchecked"})
    public YaclCategoryBuilder<T> nestedMap(String mapKey) {
        OptionInfo info = requireOptionInfo(mapKey);
        YaclMapOptionFactory.requireStringKeys(info);
        Class<?> entryType = info.getMapValueType();
        newEntry(info, entryType);
        NewEntryKeyState typedKey = new NewEntryKeyState();
        YaclOptionMetadata newKey = entryControl(info, NEW_KEY, NEW_KEY + ".desc");
        Option<String> keyField = Option.<String>createBuilder()
                .name(newKey.name())
                .description(newKey.description())
                .available(newKey.available())
                .stateManager(typedKey)
                .controller(StringControllerBuilder::create)
                .build();

        sectionHeader(info, keyField, entryButton(entryControl(info, ADD_ENTRY, ADD_MAP_ENTRY_DESC), screen -> {
            String key = typedKey.get().strip();
            ValueAccessor.mapKeyError(context.session(), info, key)
                .ifPresentOrElse(LOGGER::warn, () -> screen.rebuild(
                        () -> ValueAccessor.addMapEntry(
                                context.session(), info, key,
                                newEntry(info, entryType)
                        ).ifPresent(LOGGER::warn)
                    )
                );
        }));

        Map<?, ?> defaults = ValueAccessor.defaultMapOf(info);
        Map<String, ?> currentMap = context.session().get(info);
        if (currentMap == null) {
            currentMap = (Map<String, ?>) defaults;
        }
        if (currentMap.isEmpty()) return emptyEntriesLabel();

        for (Map.Entry<String, ?> entry : currentMap.entrySet()) {
            String entryKey = entry.getKey();
            ValueAccessor<Object> storedEntry = ValueAccessor.mapEntry(context.session(), info, entryKey);
            groups.add(buildPojoEntryGroup(info, storedEntry, defaults.get(entryKey), Component.literal(entryKey), entry.getValue(),
                    () -> ValueAccessor.removeMapEntry(context.session(), info, entryKey)));
        }
        return this;
    }

    /**
     * Like {@link #nestedMap(String)}, for a list of POJOs and without the key field. Each group
     * is named by its number, from 1, followed by the first non-blank {@code String} field among
     * {@code comment}, {@code name} and {@code id}, if there is one.
     * <p>
     * The Add Entry and Remove Entry buttons need the screen from
     * {@link YaclScreenBuilder#generateScreen()}.
     *
     * @param listKey key of a {@code List} option
     * @return this builder
     * @throws IllegalArgumentException if no option has that key
     * @throws UnsupportedOperationException if the element type has no no-args constructor or initial supplier, or a field has no controller
     */
    public YaclCategoryBuilder<T> nestedList(String listKey) {
        OptionInfo info = requireOptionInfo(listKey);
        Class<?> entryType = info.getCollectionElementType();
        newEntry(info, entryType);
        sectionHeader(info, entryButton(entryControl(info, ADD_ENTRY, ADD_ENTRY + ".desc"),
                screen -> screen.rebuild(() -> ValueAccessor.addListEntry(context.session(), info, newEntry(info, entryType)))));

        List<?> defaults = ValueAccessor.defaultListOf(info);
        List<?> currentList = context.session().get(info);
        if (currentList == null) {
            currentList = defaults;
        }
        if (currentList.isEmpty()) return emptyEntriesLabel();

        for (int i = 0; i < currentList.size(); i++) {
            int index = i;
            Object sampleItem = currentList.get(index);
            ValueAccessor<Object> storedEntry = ValueAccessor.listEntry(context.session(), info, index);
            Object defaultItem = index < defaults.size() ? defaults.get(index) : null;
            groups.add(buildPojoEntryGroup(info, storedEntry, defaultItem, resolveListEntryGroupName(index, sampleItem), sampleItem,
                    () -> ValueAccessor.removeListEntry(context.session(), info, index)));
        }
        return this;
    }

    /**
     * Adds one text {@link ListOption} per map key, each editing the list that {@code binding}
     * reads from the POJO of that key. Keys from {@code binding.keys()} come first, then the keys
     * of the map. Editing the list of a key that is not in the map creates its entry with
     * {@code binding.newEntry()}; without one, the edit is dropped. If the map has a
     * {@code @Title}, a header goes first. {@code @EnabledIf} does not apply to these lists.
     *
     * @param mapKey key of a {@code Map} option with {@code String} keys
     * @param binding how to read and write the list of each POJO
     * @param <N> the POJO type of the map values
     * @return this builder
     * @throws IllegalArgumentException if no option has that key, or the map keys are not {@code String}
     * @throws UnsupportedOperationException if the list elements can't be converted from text
     */
    public <N> YaclCategoryBuilder<T> nestedMapLists(String mapKey, NestListBinding<N> binding) {
        OptionInfo mapInfo = requireOptionInfo(mapKey);
        if (mapInfo.hasTitle()) {
            label(mapInfo.getTitle());
        }

        List<ListOption<String>> projectedLists = mapOptionFactory.buildProjectedMapListOptions(mapInfo, binding);
        if (projectedLists.isEmpty()) return emptyEntriesLabel();

        this.groups.addAll(projectedLists);
        return this;
    }

    private YaclCategoryBuilder<T> emptyEntriesLabel() {
        return label(Component.translatable(NO_ENTRIES));
    }

    /**
     * Fills the tab from the fields of the config, in declaration order with the superclass
     * fields first. Simple options in a row go into one group, and a {@code @Title} starts a new
     * group with that name. {@code @Nest} lists and maps of POJOs go to {@link #nestedList(String)}
     * and {@link #nestedMap(String)}, other lists and maps to {@link #listOption(String)} and
     * {@link #mapPairsOption(String)}, and every other {@code @Nest} field to {@link #nested(String)}.
     *
     * @return this builder
     * @throws IllegalArgumentException if a method it calls throws one, for example for an invalid {@code @EnabledIf}
     * @throws UnsupportedOperationException if an option has no controller
     */
    public YaclCategoryBuilder<T> autoGenerate() {
        CategoryNode root = context.session().getOptionTree().getSchemaRoot();
        List<String> pendingLeaves = new ArrayList<>();
        String currentSectionTitle = null;

        for (Map.Entry<String, ConfigNode> entry : root.children().entrySet()) {
            String childKey = entry.getKey();
            ConfigNode childNode = entry.getValue();

            if (childNode instanceof OptionInfo info) {
                if (info.isCollection() || info.isMap()) {
                    flushPendingLeaves(pendingLeaves, currentSectionTitle);
                    currentSectionTitle = null;
                    emitCollectionOrMapOption(info);
                } else {
                    if (info.hasTitle()) {
                        flushPendingLeaves(pendingLeaves, currentSectionTitle);
                        currentSectionTitle = info.getTitle();
                    }
                    pendingLeaves.add(childKey);
                }
            } else if (childNode instanceof CategoryNode) {
                flushPendingLeaves(pendingLeaves, currentSectionTitle);
                currentSectionTitle = null;
                nested(childKey);
            }
        }

        flushPendingLeaves(pendingLeaves, currentSectionTitle);
        return this;
    }

    private void flushPendingLeaves(List<String> pendingLeaves, String sectionTitleKey) {
        if (pendingLeaves.isEmpty()) return;
        boolean hasSectionTitle = sectionTitleKey != null && !sectionTitleKey.isBlank();
        Component groupName = hasSectionTitle
                ? context.namingStrategy().groupName(context.translationPrefix(), sectionTitleKey)
                : Component.empty();

        YaclGroupBuilder<T> builder = new YaclGroupBuilder<>(context, groupName);
        if (hasSectionTitle) {
            builder.description(context.namingStrategy().groupDescription(context.translationPrefix(), sectionTitleKey));
        }

        builder.include(pendingLeaves.toArray(String[]::new));
        groups.add(builder.build());
        pendingLeaves.clear();
    }

    private void emitCollectionOrMapOption(OptionInfo info) {
        String key = info.getKey();
        if (info.isCollection()) {
            if (info.getElementTemplate() instanceof CategoryNode) {
                nestedList(key);
            } else {
                listOption(key);
            }
        } else if (info.isMap()) {
            if (info.getElementTemplate() instanceof CategoryNode) {
                nestedMap(key);
            } else {
                mapPairsOption(key);
            }
        }
    }

    private YaclOptionMetadata entryControl(OptionInfo container, String nameKey, String descriptionKey) {
        return context.metadata(Component.translatable(nameKey), Component.translatable(descriptionKey), container, container.getKey());
    }

    private static ButtonOption entryButton(YaclOptionMetadata meta, Consumer<YaclConfigScreen> action) {
        return ButtonOption.createBuilder()
                .name(meta.name())
                .description(meta.description())
                .available(meta.available())
                .text(Component.empty())
                .action((screen, button) -> action.accept(YaclConfigScreen.from(screen)))
                .build();
    }

    private Object newEntry(OptionInfo container, Class<?> entryType) {
        Object entry = context.controllerRegistry().resolveInitialValue(entryType);
        if (entry == null) {
            throw new UnsupportedOperationException("Add Entry needs a no-args constructor or an initial supplier for "
                    + entryType.getName() + " on config option: " + container.getKey());
        }
        return entry;
    }

    private void sectionHeader(OptionInfo container, Option<?>... controls) {
        List<Option<?>> options = new ArrayList<>();
        if (container.hasTitle()) {
            options.add(LabelOption.create(context.namingStrategy().groupName(context.translationPrefix(), container.getTitle())));
        }
        options.addAll(List.of(controls));
        groups.add(new YaclSectionHeaderGroup(ImmutableList.copyOf(options)));
    }

    private OptionGroup buildPojoEntryGroup(
            OptionInfo containerInfo,
            ValueAccessor<Object> storedEntry,
            Object defaultItem,
            Component groupName,
            Object sampleItem,
            Runnable removal
    ) {
        OptionGroup.Builder groupBuilder = OptionGroup.createBuilder()
                .name(groupName)
                .collapsed(true);

        List<Option<?>> groupOptions = new ArrayList<>();
        if (sampleItem != null) {
            for (Field field : resolvePojoFields(sampleItem.getClass())) {
                if (field.isAnnotationPresent(Title.class)) {
                    String titleVal = field.getAnnotation(Title.class).value();
                    if (titleVal != null && !titleVal.isBlank()) {
                        groupOptions.add(LabelOption.create(context.namingStrategy().groupName(context.translationPrefix(), titleVal)));
                    }
                }
                groupOptions.add(YaclPojoFieldBuilder.buildPojoFieldOption(context, containerInfo, storedEntry, defaultItem, field));
            }
        }

        groupOptions.add(entryButton(entryControl(containerInfo, REMOVE_ENTRY, REMOVE_ENTRY + ".desc"), screen -> screen.rebuild(removal)));
        groupBuilder.options(groupOptions);
        return groupBuilder.build();
    }

    private static List<Field> resolvePojoFields(Class<?> itemClass) {
        return ReflectionUtils.declaredFieldsSuperclassFirst(itemClass).stream()
                .filter(field -> !OptionTree.isIgnored(field))
                .toList();
    }

    private static Component resolveListEntryGroupName(int index, Object sampleItem) {
        if (sampleItem != null) {
            for (String candidateField : List.of("comment", "name", "id")) {
                Field f = ReflectionUtils.findField(sampleItem.getClass(), candidateField);
                if (f != null) {
                    Object val = ReflectionUtils.getFieldValue(f, sampleItem);
                    if (val instanceof String s && !s.isBlank()) {
                        return Component.literal("#" + (index + 1) + ": " + s);
                    }
                }
            }
        }
        return Component.literal("#" + (index + 1));
    }

    private OptionInfo requireOptionInfo(String key) {
        OptionInfo info = context.session().getOptionTree().get(key);
        if (info == null) {
            throw new IllegalArgumentException("Unknown config option key: " + key);
        }
        return info;
    }

    ConfigCategory build() {
        ConfigCategory.Builder builder = ConfigCategory.createBuilder()
                .name(name)
                .groups(groups);

        if (description != null) builder.tooltip(description);
        return builder.build();
    }
}
