package com.stalemated.lib.compat.yacl.builder;

import com.stalemated.lib.compat.yacl.internal.ValueAccessor;
import com.stalemated.lib.compat.yacl.internal.YaclContext;
import com.stalemated.lib.compat.yacl.internal.YaclMapOptionFactory;
import com.stalemated.lib.compat.yacl.internal.YaclPojoFieldBuilder;
import com.stalemated.lib.config.annotation.Title;
import com.stalemated.lib.config.internal.CategoryNode;
import com.stalemated.lib.config.internal.ConfigNode;
import com.stalemated.lib.config.OptionInfo;
import com.stalemated.lib.config.OptionTree;
import com.stalemated.lib.util.internal.ReflectionUtils;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionGroup;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

/**
 * Fluent builder for YACL {@link ConfigCategory}.
 * Represents a single category tab in the configuration screen.
 *
 * @param <T> The config model type.
 */
@SuppressWarnings("UnusedReturnValue")
public class YaclCategoryBuilder<T> {
    private static final String NO_ENTRIES = "s_lib.config_screen.no_entries";

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

    public YaclCategoryBuilder<T> name(Component name) {
        this.name = name;
        return this;
    }

    public YaclCategoryBuilder<T> description(Component description) {
        this.description = description;
        return this;
    }

    public YaclCategoryBuilder<T> group(OptionGroup customGroup) {
        this.groups.add(customGroup);
        return this;
    }

    public YaclCategoryBuilder<T> group(String groupKey, Consumer<YaclGroupBuilder<T>> consumer) {
        Component groupName = context.namingStrategy().groupName(context.translationPrefix(), groupKey);
        YaclGroupBuilder<T> builder = new YaclGroupBuilder<>(context, groupName);
        builder.description(context.namingStrategy().groupDescription(context.translationPrefix(), groupKey));
        consumer.accept(builder);
        groups.add(builder.build());
        return this;
    }

    /**
     * Emits a standalone {@link LabelOption} section header group in this category
     * without a collapsible arrow bar.
     *
     * @param labelText The label component to display.
     * @return This category builder.
     */
    public YaclCategoryBuilder<T> label(Component labelText) {
        groups.add(YaclSectionHeaderGroup.of(labelText));
        return this;
    }

    /**
     * Emits a standalone {@link LabelOption} section header group resolved via the naming strategy.
     *
     * @param titleKey The translation key or literal title text.
     * @return This category builder.
     */
    public YaclCategoryBuilder<T> label(String titleKey) {
        return label(context.namingStrategy().groupName(context.translationPrefix(), titleKey));
    }

    public YaclCategoryBuilder<T> listOption(String key) {
        return listOption(key, null);
    }

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

    public YaclCategoryBuilder<T> mapPairsOption(String key) {
        return mapPairsOption(key, null);
    }

    public YaclCategoryBuilder<T> mapPairsOption(String key, Consumer<ListOption.Builder<String>> customizer) {
        OptionInfo info = requireOptionInfo(key);
        if (info.hasTitle()) {
            label(info.getTitle());
        }
        ListOption<String> mapOpt = mapOptionFactory.buildMapPairsOption(info, customizer);
        groups.add(mapOpt);
        return this;
    }

    public YaclCategoryBuilder<T> nested(String nestPrefix) {
        return nested(nestPrefix, null);
    }

    public YaclCategoryBuilder<T> nested(String nestPrefix, Consumer<YaclGroupBuilder<T>> customizer) {
        CategoryNode catNode = resolveCategoryNode(nestPrefix);
        String groupKey = catNode != null && catNode.hasTitle() ? catNode.title() : nestPrefix;
        YaclGroupBuilder<T> builder = new YaclGroupBuilder<>(context, context.namingStrategy().groupName(context.translationPrefix(), groupKey));
        builder.collapsed(false);
        builder.description(context.namingStrategy().groupDescription(context.translationPrefix(), groupKey));
        builder.includePrefix(nestPrefix);
        if (customizer != null) {
            customizer.accept(builder);
        }
        OptionGroup builtGroup = builder.build();
        if (!builtGroup.options().isEmpty()) {
            groups.add(builtGroup);
        }

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
     * Maps entries of a nested POJO map into individual collapsible option groups.
     *
     * @param mapKey The key of the map option.
     * @return This category builder.
     */
    @SuppressWarnings({"unchecked"})
    public YaclCategoryBuilder<T> nestedMap(String mapKey) {
        OptionInfo info = requireOptionInfo(mapKey);
        if (info.hasTitle()) {
            label(info.getTitle());
        }

        Map<?, ?> defaults = ValueAccessor.defaultMapOf(info);
        Map<String, ?> currentMap = context.session().get(info);
        if (currentMap == null) {
            currentMap = (Map<String, ?>) defaults;
        }
        if (currentMap.isEmpty()) return emptyEntriesLabel();

        for (Map.Entry<String, ?> entry : currentMap.entrySet()) {
            String entryKey = entry.getKey();
            ValueAccessor<Object> storedEntry = ValueAccessor.mapEntry(context.session(), info, entryKey);
            groups.add(buildPojoEntryGroup(info, storedEntry, defaults.get(entryKey), Component.literal(entryKey), entry.getValue()));
        }
        return this;
    }

    /**
     * Maps elements of a nested POJO list into individual collapsible option groups.
     *
     * @param listKey The key of the list option.
     * @return This category builder.
     */
    public YaclCategoryBuilder<T> nestedList(String listKey) {
        OptionInfo info = requireOptionInfo(listKey);
        if (info.hasTitle()) {
            label(info.getTitle());
        }

        List<?> defaults = ValueAccessor.defaultListOf(info);
        List<?> currentList = context.session().get(info);
        if (currentList == null) {
            currentList = defaults;
        }
        if (currentList.isEmpty()) return emptyEntriesLabel();

        for (int i = 0; i < currentList.size(); i++) {
            Object sampleItem = currentList.get(i);
            ValueAccessor<Object> storedEntry = ValueAccessor.listEntry(context.session(), info, i);
            Object defaultItem = i < defaults.size() ? defaults.get(i) : null;
            groups.add(buildPojoEntryGroup(info, storedEntry, defaultItem, resolveListEntryGroupName(i, sampleItem), sampleItem));
        }
        return this;
    }

    /**
     * Maps dynamic list options inside entries of a nested POJO map using a {@link NestListBinding}.
     *
     * @param mapKey The key of the map option.
     * @param binding The functional binding for reading and writing the list property on each POJO item.
     * @param <N> The type of the nested POJO item.
     * @return This category builder.
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
     * Automatically discovers all options, collections, maps, and nested POJO categories
     * from the {@link OptionTree} schema in declaration order.
     *
     * @return This category builder.
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

    private CategoryNode resolveCategoryNode(String nestPrefix) {
        ConfigNode current = context.session().getOptionTree().getSchemaRoot();
        for (String part : nestPrefix.split("\\.")) {
            if (current instanceof CategoryNode catNode) {
                current = catNode.getChild(part);
            } else {
                return null;
            }
        }
        return current instanceof CategoryNode cat ? cat : null;
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

    private OptionGroup buildPojoEntryGroup(
            OptionInfo containerInfo,
            ValueAccessor<Object> storedEntry,
            Object defaultItem,
            Component groupName,
            Object sampleItem
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

        groupBuilder.options(groupOptions);
        return groupBuilder.build();
    }

    private static List<Field> resolvePojoFields(Class<?> itemClass) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = itemClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!OptionTree.isIgnored(field)) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
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

    public ConfigCategory build() {
        ConfigCategory.Builder builder = ConfigCategory.createBuilder()
                .name(name)
                .groups(groups);

        if (description != null) builder.tooltip(description);
        return builder.build();
    }
}
