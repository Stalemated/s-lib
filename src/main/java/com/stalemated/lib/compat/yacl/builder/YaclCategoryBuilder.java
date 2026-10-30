package com.stalemated.lib.compat.yacl.builder;

import com.stalemated.lib.compat.yacl.factory.YaclContext;
import com.stalemated.lib.compat.yacl.factory.YaclListOptionFactory;
import com.stalemated.lib.compat.yacl.factory.YaclMapOptionFactory;
import com.stalemated.lib.compat.yacl.factory.YaclOptionFactory;
import com.stalemated.lib.compat.yacl.util.NestedAccessor;
import com.stalemated.lib.config.annotation.Title;
import com.stalemated.lib.config.model.CategoryNode;
import com.stalemated.lib.config.model.ConfigNode;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;
import com.stalemated.lib.util.reflection.ReflectionUtils;
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
    private final YaclContext<T> context;
    private final YaclOptionFactory<T> optionFactory;
    private final YaclListOptionFactory<T> listOptionFactory;
    private final YaclMapOptionFactory<T> mapOptionFactory;

    private Component name;
    private Component tooltip;
    private final List<OptionGroup> groups = new ArrayList<>();

    public YaclCategoryBuilder(YaclContext<T> context) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.optionFactory = new YaclOptionFactory<>(context);
        this.listOptionFactory = new YaclListOptionFactory<>(context);
        this.mapOptionFactory = new YaclMapOptionFactory<>(context);
        this.name = Component.literal(context.session().getManager().getName());
    }

    public YaclContext<T> context() {
        return context;
    }

    public YaclCategoryBuilder<T> name(Component name) {
        this.name = name;
        return this;
    }

    public YaclCategoryBuilder<T> tooltip(Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public YaclCategoryBuilder<T> group(OptionGroup customGroup) {
        this.groups.add(customGroup);
        return this;
    }

    public YaclCategoryBuilder<T> group(String groupKey, Consumer<YaclGroupBuilder<T>> consumer) {
        Component groupName = context.namingStrategy().resolveGroupName(context.modId(), groupKey);
        YaclGroupBuilder<T> builder = new YaclGroupBuilder<>(context, groupName);
        builder.description(context.namingStrategy().resolveGroupDescription(context.modId(), groupKey));
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
        return label(context.namingStrategy().resolveTitle(context.modId(), titleKey));
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
        ListOption<?> listOpt = listOptionFactory.buildListOption(info, (Consumer) customizer);
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

    public YaclCategoryBuilder<T> nestedGroup(String nestPrefix) {
        return nestedGroup(nestPrefix, null);
    }

    public YaclCategoryBuilder<T> nestedGroup(String nestPrefix, Consumer<YaclGroupBuilder<T>> customizer) {
        CategoryNode catNode = resolveCategoryNode(nestPrefix);
        Component groupName = (catNode != null && catNode.hasTitle())
                ? context.namingStrategy().resolveTitle(context.modId(), catNode.title())
                : context.namingStrategy().resolveGroupName(context.modId(), nestPrefix);
        YaclGroupBuilder<T> builder = new YaclGroupBuilder<>(context, groupName);
        builder.collapsed(false);
        if (catNode != null && catNode.hasTitle()) {
            builder.description(context.namingStrategy().resolveTitleDescription(context.modId(), catNode.title()));
        } else {
            builder.description(context.namingStrategy().resolveGroupDescription(context.modId(), nestPrefix));
        }
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
    public YaclCategoryBuilder<T> mapNestGroups(String mapKey) {
        OptionInfo info = requireOptionInfo(mapKey);
        if (info.hasTitle()) {
            label(info.getTitle());
        }
        boolean available = context.availabilityRule().isAvailable(info, context.session());

        Map<String, ?> currentMap = context.session().get(info);
        if (currentMap == null && info.getDefaultValue() instanceof Map<?, ?> defaultMap) {
            currentMap = (Map<String, ?>) defaultMap;
        }
        if (currentMap == null || currentMap.isEmpty()) return emptyEntriesLabel(info);

        for (Map.Entry<String, ?> entry : currentMap.entrySet()) {
            String entryKey = entry.getKey();
            Object sampleItem = entry.getValue();
            groups.add(buildPojoEntryGroup(info, NestedAccessor.forMap(entryKey), Component.literal(entryKey), sampleItem, available));
        }
        return this;
    }

    /**
     * Maps elements of a nested POJO list into individual collapsible option groups.
     *
     * @param listKey The key of the list option.
     * @return This category builder.
     */
    public YaclCategoryBuilder<T> listNestGroups(String listKey) {
        OptionInfo info = requireOptionInfo(listKey);
        if (info.hasTitle()) {
            label(info.getTitle());
        }
        boolean available = context.availabilityRule().isAvailable(info, context.session());

        List<?> currentList = context.session().get(info);
        if (currentList == null && info.getDefaultValue() instanceof List<?> defaultList) {
            currentList = defaultList;
        }
        if (currentList == null || currentList.isEmpty()) return emptyEntriesLabel(info);

        for (int i = 0; i < currentList.size(); i++) {
            Object sampleItem = currentList.get(i);
            Component groupName = resolveListEntryGroupName(i, sampleItem);
            groups.add(buildPojoEntryGroup(info, NestedAccessor.forList(i), groupName, sampleItem, available));
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
    public <N> YaclCategoryBuilder<T> mapNestListGroups(String mapKey, NestListBinding<N> binding) {
        OptionInfo mapInfo = context.session().getOptionTree().get(mapKey);
        if (mapInfo == null) return this;

        List<ListOption<String>> projectedLists = mapOptionFactory.buildProjectedMapListOptions(mapInfo, binding);
        if (projectedLists.isEmpty()) return emptyEntriesLabel(mapInfo);

        this.groups.addAll(projectedLists);
        return this;
    }

    private YaclCategoryBuilder<T> emptyEntriesLabel(OptionInfo info) {
        return label(context.namingStrategy().resolveEmptyEntries(context.modId(), info));
    }

    /**
     * Automatically discovers all options, collections, maps, and nested POJO categories
     * from the {@link OptionTree} schema in declaration order.
     *
     * @return This category builder.
     */
    public YaclCategoryBuilder<T> autoGenerate() {
        ConfigNode root = context.session().getOptionTree().getSchemaRoot();

        if (root instanceof CategoryNode categoryNode) {
            List<String> pendingLeaves = new ArrayList<>();
            String currentSectionTitle = null;

            for (Map.Entry<String, ConfigNode> entry : categoryNode.children().entrySet()) {
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
                    nestedGroup(childKey);
                }
            }

            flushPendingLeaves(pendingLeaves, currentSectionTitle);
        }
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
                ? context.namingStrategy().resolveTitle(context.modId(), sectionTitleKey)
                : Component.empty();

        YaclGroupBuilder<T> builder = new YaclGroupBuilder<>(context, groupName);
        if (hasSectionTitle) {
            builder.description(context.namingStrategy().resolveTitleDescription(context.modId(), sectionTitleKey));
        }

        builder.include(pendingLeaves.toArray(String[]::new));
        groups.add(builder.build());
        pendingLeaves.clear();
    }

    private void emitCollectionOrMapOption(OptionInfo info) {
        String key = info.getKey();
        if (info.isCollection()) {
            if (info.getElementTemplate() instanceof CategoryNode) {
                listNestGroups(key);
            } else {
                listOption(key);
            }
        } else if (info.isMap()) {
            if (info.getElementTemplate() instanceof CategoryNode) {
                mapNestGroups(key);
            } else {
                mapPairsOption(key);
            }
        }
    }

    private OptionGroup buildPojoEntryGroup(
            OptionInfo containerInfo,
            NestedAccessor accessor,
            Component groupName,
            Object sampleItem,
            boolean available
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
                        groupOptions.add(LabelOption.create(context.namingStrategy().resolveTitle(context.modId(), titleVal)));
                    }
                }
                Option<?> opt = this.optionFactory.buildPojoFieldOption(containerInfo, accessor, field, sampleItem, available);
                if (opt != null) {
                    groupOptions.add(opt);
                }
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

        if (tooltip != null) builder.tooltip(tooltip);
        return builder.build();
    }
}
