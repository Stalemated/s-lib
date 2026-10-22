package com.stalemated.lib.compat.yacl.builder;

import com.stalemated.lib.compat.yacl.factory.YaclContext;
import com.stalemated.lib.compat.yacl.factory.YaclListOptionFactory;
import com.stalemated.lib.compat.yacl.factory.YaclMapOptionFactory;
import com.stalemated.lib.compat.yacl.factory.YaclOptionFactory;
import com.stalemated.lib.compat.yacl.util.YaclSessionHelper;
import com.stalemated.lib.config.model.CategoryNode;
import com.stalemated.lib.config.model.ConfigNode;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Fluent builder for YACL {@link ConfigCategory}.
 * Represents a single category tab in the configuration screen.
 *
 * @param <T> The config model type.
 */
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

    @SuppressWarnings("UnusedReturnValue")
    public YaclCategoryBuilder<T> name(Component name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclCategoryBuilder<T> tooltip(Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclCategoryBuilder<T> group(OptionGroup customGroup) {
        this.groups.add(customGroup);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclCategoryBuilder<T> group(String groupKey, Consumer<YaclGroupBuilder<T>> consumer) {
        Component groupName = context.namingStrategy().resolveGroupName(context.modId(), groupKey);
        YaclGroupBuilder<T> builder = new YaclGroupBuilder<>(context, groupName);
        consumer.accept(builder);
        groups.add(builder.build());
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclCategoryBuilder<T> listOption(String key) {
        return listOption(key, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes", "UnusedReturnValue"})
    public YaclCategoryBuilder<T> listOption(String key, Consumer<ListOption.Builder<?>> customizer) {
        OptionInfo info = context.session().getOptionTree().get(key);
        if (info == null) {
            throw new IllegalArgumentException("Unknown config option key: " + key);
        }
        ListOption<?> listOpt = listOptionFactory.buildListOption(info, (Consumer) customizer);
        groups.add(listOpt);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclCategoryBuilder<T> mapPairsOption(String key) {
        return mapPairsOption(key, null);
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclCategoryBuilder<T> mapPairsOption(String key, Consumer<ListOption.Builder<String>> customizer) {
        OptionInfo info = context.session().getOptionTree().get(key);
        if (info == null) {
            throw new IllegalArgumentException("Unknown config option key: " + key);
        }
        ListOption<String> mapOpt = mapOptionFactory.buildMapPairsOption(info, customizer);
        groups.add(mapOpt);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclCategoryBuilder<T> nestedGroup(String nestPrefix) {
        return nestedGroup(nestPrefix, null);
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclCategoryBuilder<T> nestedGroup(String nestPrefix, Consumer<YaclGroupBuilder<T>> customizer) {
        Component groupName = context.namingStrategy().resolveGroupName(context.modId(), nestPrefix);
        YaclGroupBuilder<T> builder = new YaclGroupBuilder<>(context, groupName);
        builder.collapsed(true);
        builder.includePrefix(nestPrefix);
        if (customizer != null) {
            customizer.accept(builder);
        }
        groups.add(builder.build());
        return this;
    }

    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public YaclCategoryBuilder<T> mapNestGroups(String mapKey) {
        OptionInfo info = context.session().getOptionTree().get(mapKey);
        if (info == null) throw new IllegalArgumentException("Unknown config option key: " + mapKey);

        boolean available = context.availabilityRule().isAvailable(info, context.session());

        Map<String, ?> currentMap = context.session().get(info);
        if (currentMap == null || currentMap.isEmpty()) {
            if (info.getDefaultValue() instanceof Map<?, ?> defaultMap) {
                currentMap = (Map<String, ?>) defaultMap;
            }
        }
        if (currentMap == null || currentMap.isEmpty()) return this;

        for (String entryKey : currentMap.keySet()) {
            Component groupName = Component.literal(entryKey);
            OptionGroup.Builder groupBuilder = OptionGroup.createBuilder()
                    .name(groupName)
                    .collapsed(true);

            List<Option<?>> groupOptions = new ArrayList<>();
            Object sampleItem = currentMap.get(entryKey);

            if (sampleItem != null) {
                Class<?> itemClass = sampleItem.getClass();
                for (Field field : itemClass.getDeclaredFields()) {
                    if (OptionTree.isIgnored(field)) continue;

                    Option<?> opt = this.optionFactory.buildPojoFieldOption(info, entryKey, field, sampleItem, available);
                    if (opt != null) groupOptions.add(opt);
                }
            }

            groupBuilder.options(groupOptions);
            groups.add(groupBuilder.build());
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
    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public <N> YaclCategoryBuilder<T> mapNestListGroups(String mapKey, NestListBinding<N> binding) {
        OptionInfo mapInfo = context.session().getOptionTree().get(mapKey);
        if (mapInfo != null) {
            boolean available = context.availabilityRule().isAvailable(mapInfo, context.session());

            Map<String, N> currentMap = context.session().get(mapInfo);
            if (currentMap == null && mapInfo.getDefaultValue() instanceof Map<?, ?> defMap) {
                currentMap = (Map<String, N>) defMap;
            }

            if (currentMap != null) {
                for (String entryKey : currentMap.keySet()) {
                    Component name = Component.literal(entryKey);
                    OptionDescription desc = context.namingStrategy().resolveDescription(context.modId(), mapInfo, available);
                    List<String> defaultList = resolveDefaultList(mapInfo, entryKey, binding.getter());

                    ListOption<String> listOpt = createNestListOption(
                            name, desc, available, defaultList, mapInfo, entryKey, binding
                    );

                    this.groups.add(listOpt);
                }
            }
        }
        return this;
    }

    private <N> ListOption<String> createNestListOption(
            Component name,
            OptionDescription desc,
            boolean available,
            List<String> defaultList,
            OptionInfo mapInfo,
            String entryKey,
            NestListBinding<N> binding
    ) {
        return ListOption.<String>createBuilder()
                .name(name)
                .description(desc)
                .available(available)
                .binding(
                        defaultList,
                        () -> getNestedList(mapInfo, entryKey, binding.getter()),
                        val -> updateNestedList(mapInfo, entryKey, val, binding.setter())
                )
                .controller(StringControllerBuilder::create)
                .initial("")
                .build();
    }

    @SuppressWarnings("unchecked")
    private <N> List<String> resolveDefaultList(
            OptionInfo mapInfo,
            String entryKey,
            Function<N, List<String>> listGetter
    ) {
        if (mapInfo.getDefaultValue() instanceof Map<?, ?> defMap && defMap.containsKey(entryKey)) {
            N defItem = (N) defMap.get(entryKey);
            if (defItem != null) {
                List<String> l = listGetter.apply(defItem);
                if (l != null) return new ArrayList<>(l);
            }
        }
        return new ArrayList<>();
    }

    private <N> List<String> getNestedList(
            OptionInfo mapInfo,
            String entryKey,
            Function<N, List<String>> listGetter
    ) {
        Map<String, N> m = context.session().get(mapInfo);
        if (m != null && m.containsKey(entryKey)) {
            List<String> l = listGetter.apply(m.get(entryKey));
            return l != null ? new ArrayList<>(l) : new ArrayList<>();
        }
        return new ArrayList<>();
    }

    private <N> void updateNestedList(
            OptionInfo mapInfo,
            String entryKey,
            List<String> val,
            BiConsumer<N, List<String>> listSetter
    ) {
        YaclSessionHelper.<N>updateNestedMap(context.session(), mapInfo, entryKey, item -> listSetter.accept(item, new ArrayList<>(val)));
    }

    /**
     * Automatically discovers all options and nested POJO categories from the OptionTree schema.
     */
    @SuppressWarnings("UnusedReturnValue")
    public YaclCategoryBuilder<T> autoGenerate() {
        ConfigNode root = context.session().getOptionTree().getSchemaRoot();

        if (root instanceof CategoryNode categoryNode) {
            List<String> rootLeaves = new ArrayList<>();
            List<String> subcategories = new ArrayList<>();

            for (Map.Entry<String, ConfigNode> entry : categoryNode.children().entrySet()) {
                dispatchChildNode(entry, rootLeaves, subcategories);
            }

            if (!rootLeaves.isEmpty()) {
                group("general", g -> g.include(rootLeaves.toArray(String[]::new)));
            }

            for (String subKey : subcategories) {
                nestedGroup(subKey);
            }
        }
        return this;
    }

    private void dispatchChildNode(Map.Entry<String, ConfigNode> entry, List<String> rootLeaves, List<String> subcategories) {
        String childKey = entry.getKey();
        ConfigNode childNode = entry.getValue();

        if (childNode instanceof OptionInfo info) {
            if (info.isCollection()) {
                listOption(childKey);
            } else if (info.isMap()) {
                if (info.getElementTemplate() instanceof CategoryNode) {
                    mapNestGroups(childKey);
                } else {
                    mapPairsOption(childKey);
                }
            } else {
                rootLeaves.add(childKey);
            }
        } else if (childNode instanceof CategoryNode) {
            subcategories.add(childKey);
        }
    }

    public ConfigCategory build() {
        ConfigCategory.Builder builder = ConfigCategory.createBuilder()
                .name(name)
                .groups(groups);

        if (tooltip != null) builder.tooltip(tooltip);
        return builder.build();
    }
}
