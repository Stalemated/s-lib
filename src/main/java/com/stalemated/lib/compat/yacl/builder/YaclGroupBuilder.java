package com.stalemated.lib.compat.yacl.builder;

import com.stalemated.lib.compat.yacl.controller.builder.SimpleStringDropdownControllerBuilder;
import com.stalemated.lib.compat.yacl.factory.YaclContext;
import com.stalemated.lib.compat.yacl.factory.YaclOptionFactory;
import com.stalemated.lib.compat.yacl.registry.YaclControllerRegistry;
import com.stalemated.lib.compat.yacl.util.YaclBindingSynchronizer;
import com.stalemated.lib.config.model.OptionInfo;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.controller.ValueFormatter;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Fluent builder for YACL {@link OptionGroup}.
 * Groups individual options together under a titled and collapsible group.
 *
 * @param <T> The config model type.
 */
@SuppressWarnings("UnusedReturnValue")
public class YaclGroupBuilder<T> {
    private final YaclContext<T> context;
    private final YaclOptionFactory<T> optionFactory;
    private Component name;
    private OptionDescription description;
    private boolean collapsed = false;
    private final List<Consumer<List<Option<?>>>> actions = new ArrayList<>();
    private final Map<String, Consumer<Option.Builder<?>>> overrides = new HashMap<>();

    public YaclGroupBuilder(YaclContext<T> context, Component defaultName) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.optionFactory = new YaclOptionFactory<>(context);
        this.name = defaultName;
    }

    public YaclContext<T> context() {
        return context;
    }

    public YaclGroupBuilder<T> name(Component name) {
        this.name = name;
        return this;
    }

    public YaclGroupBuilder<T> description(OptionDescription description) {
        this.description = description;
        return this;
    }

    public YaclGroupBuilder<T> description(Component description) {
        this.description = OptionDescription.of(description);
        return this;
    }

    public YaclGroupBuilder<T> collapsed(boolean collapsed) {
        this.collapsed = collapsed;
        return this;
    }

    public YaclGroupBuilder<T> option(Option<?> customOption) {
        this.actions.add(list -> list.add(customOption));
        return this;
    }

    /**
     * Appends a YACL {@link LabelOption} section header to this group.
     *
     * @param labelText The text component to display.
     * @return This group builder.
     */
    public YaclGroupBuilder<T> label(Component labelText) {
        this.actions.add(list -> list.add(LabelOption.create(labelText)));
        return this;
    }

    /**
     * Appends a YACL {@link LabelOption} section header resolved via the naming strategy.
     *
     * @param titleKey The translation key or literal title string.
     * @return This group builder.
     */
    public YaclGroupBuilder<T> label(String titleKey) {
        Component resolved = context.namingStrategy().resolveTitle(context.modId(), titleKey);
        return label(resolved);
    }

    public YaclGroupBuilder<T> configureOption(String key, Consumer<Option.Builder<?>> customizer) {
        this.overrides.put(key, customizer);
        return this;
    }

    /**
     * Configures a {@code String} or {@code ResourceLocation} option
     * to render as a dropdown populated with the given string values, and includes it in this group.
     *
     * @param key    The option key.
     * @param values The list of selectable string values.
     * @return This group builder.
     */
    public YaclGroupBuilder<T> dropdown(String key, List<String> values) {
        return dropdown(key, values, Component::literal);
    }

    /**
     * Configures a {@code String} or {@code ResourceLocation} option
     * to render as a dropdown with a custom display formatter, and includes it in this group.
     *
     * @param key       The option key.
     * @param values    The list of selectable string values.
     * @param formatter Custom display formatter for each string entry.
     * @return This group builder.
     */
    @SuppressWarnings("unchecked")
    public YaclGroupBuilder<T> dropdown(String key, List<String> values, ValueFormatter<String> formatter) {
        configureOption(key, b -> ((Option.Builder<String>) b).controller(opt ->
                SimpleStringDropdownControllerBuilder.create(opt)
                        .values(values)
                        .formatValue(formatter)
        ));
        return include(key);
    }

    /**
     * Projects a single element at {@code index} of a {@code List<E>} configuration option
     * as a standalone YACL {@link Option} inside this group (e.g., primary/secondary colors in a 2-color gradient list).
     *
     * @param listKey        The key of the backing {@code List<E>} option in the config model.
     * @param index          The zero-based index within the list.
     * @param optionKey      The logical option key used to resolve the display name and description via the naming strategy.
     * @param defaultElement The default element value when the list does not yet contain an entry at {@code index}.
     * @param <E>            The element type of the list.
     * @return This group builder.
     */
    public <E> YaclGroupBuilder<T> listElement(String listKey, int index, String optionKey, E defaultElement) {
        this.actions.add(list -> list.add(buildListElementOption(listKey, index, optionKey, defaultElement)));
        return this;
    }

    public YaclGroupBuilder<T> include(String... keys) {
        String[] keysCopy = keys.clone();
        actions.add(list -> {
            for (String key : keysCopy) {
                OptionInfo info = context.session().getOptionTree().get(key);
                if (info == null) {
                    throw new IllegalArgumentException("Unknown config option key: '" + key + "' in OptionGroup '"
                            + (name != null ? name.getString() : "unnamed") + "'.");
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

    public YaclGroupBuilder<T> includePrefix(String prefix) {
        String cleanPrefix = prefix.endsWith(".") ? prefix : prefix + ".";
        actions.add(list -> {
            for (OptionInfo info : context.session().getOptionTree().all()) {
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <E> Option<?> buildListElementOption(String listKey, int index, String optionKey, E defaultElement) {
        OptionInfo listInfo = context.session().getOptionTree().get(listKey);
        if (listInfo == null || !List.class.isAssignableFrom(listInfo.type())) {
            throw new IllegalArgumentException("Unknown or non-List option key for listElement: '" + listKey + "'");
        }

        Class<?> elemClass = defaultElement != null ? defaultElement.getClass() : listInfo.getCollectionElementType();
        boolean available = context.availabilityRule().isAvailable(listInfo, context.session());
        Component optName = context.namingStrategy().resolveName(context.modId(), optionKey);
        OptionDescription optDesc = context.namingStrategy().resolveDescription(context.modId(), optionKey, available);

        OptionInfo syntheticElementInfo = new OptionInfo(
                listKey + "[" + index + "]",
                listInfo.getField(),
                Collections.emptyList(),
                listInfo.getSyncMode(),
                defaultElement,
                elemClass,
                elemClass,
                true,
                null
        );

        YaclControllerRegistry.AdaptedType adapted = context.controllerRegistry().findAdaptedType(elemClass);
        if (adapted != null) {
            return buildAdaptedListElementOption(listInfo, syntheticElementInfo, index, defaultElement, optName, optDesc, available, adapted);
        }

        YaclControllerRegistry.ControllerFactory direct = context.controllerRegistry().findDirectController(elemClass);
        if (direct != null) {
            return buildDirectListElementOption(listInfo, syntheticElementInfo, index, defaultElement, optName, optDesc, available, direct);
        }

        throw new UnsupportedOperationException("No YACL controller or adapter registered for list element type: "
                + (elemClass != null ? elemClass.getName() : "null") + " on list option: " + listKey);
    }

    private <E, G> Option<G> buildAdaptedListElementOption(
            OptionInfo listInfo, OptionInfo syntheticElementInfo, int index,
            E defaultElement, Component optName, OptionDescription optDesc,
            boolean available, YaclControllerRegistry.AdaptedType<E, G> adapted
    ) {
        G guiDefault = defaultElement != null
                ? adapted.toGui().apply(defaultElement)
                : adapted.fallbackGuiDefault();
        E resolvedModelDefault = defaultElement != null
                ? defaultElement
                : adapted.fromGui().apply(guiDefault);

        return buildSyncedListElementOption(
                listInfo, syntheticElementInfo, index,
                resolvedModelDefault, guiDefault,
                adapted.toGui(), adapted.fromGui(),
                optName, optDesc, available,
                adapted.guiControllerFactory());
    }

    private <E> Option<E> buildDirectListElementOption(
            OptionInfo listInfo, OptionInfo syntheticElementInfo, int index,
            E defaultElement, Component optName, OptionDescription optDesc,
            boolean available, YaclControllerRegistry.ControllerFactory<E> direct
    ) {
        E resolvedDefault = defaultElement != null
                ? defaultElement
                : context.controllerRegistry().resolveInitialValue(syntheticElementInfo.type());

        return buildSyncedListElementOption(
                listInfo, syntheticElementInfo, index,
                resolvedDefault, resolvedDefault,
                Function.identity(), Function.identity(),
                optName, optDesc, available,
                direct);
    }

    private <E, G> Option<G> buildSyncedListElementOption(
            OptionInfo listInfo,
            OptionInfo syntheticElementInfo,
            int index,
            E resolvedModelDefault,
            G guiDefault,
            Function<E, G> toGui,
            Function<G, E> fromGui,
            Component optName,
            OptionDescription optDesc,
            boolean available,
            YaclControllerRegistry.ControllerFactory<G> controllerFactory
    ) {
        Option.Builder<G> builder = Option.<G>createBuilder()
                .name(optName)
                .description(optDesc)
                .available(available);

        return YaclBindingSynchronizer.buildSyncedOption(
                context.session(),
                syntheticElementInfo,
                builder,
                guiDefault,
                () -> {
                    List<E> list = context.session().get(listInfo);
                    if (list != null && list.size() > index && list.get(index) != null) {
                        G guiVal = toGui.apply(list.get(index));
                        if (guiVal != null) return guiVal;
                    }
                    return guiDefault;
                },
                guiVal -> {
                    List<E> current = context.session().get(listInfo);
                    List<E> updated = current != null ? new ArrayList<>(current) : new ArrayList<>();
                    while (updated.size() <= index) {
                        updated.add(resolvedModelDefault);
                    }
                    E modelVal = fromGui.apply(guiVal);
                    updated.set(index, modelVal != null ? modelVal : resolvedModelDefault);
                    context.session().set(listInfo, updated);
                },
                b -> b.controller(opt -> controllerFactory.create(syntheticElementInfo, opt))
        );
    }

    private boolean hasGroupTitle() {
        return name != null && !name.equals(Component.empty()) && !name.getString().isEmpty();
    }

    private void appendOptionWithTitle(List<Option<?>> list, OptionInfo info) {
        if (info.hasTitle()) {
            Component titleComp = context.namingStrategy().resolveTitle(context.modId(), info.getTitle());
            if (list.isEmpty()) {
                if (!hasGroupTitle()) {
                    this.name = titleComp;
                }
            } else {
                list.add(LabelOption.create(titleComp));
            }
        }
        Consumer<Option.Builder<?>> customizer = overrides.get(info.getKey());
        list.add(optionFactory.buildOption(info, customizer));
    }

    public OptionGroup build() {
        List<Option<?>> resolvedOptions = new ArrayList<>();
        for (Consumer<List<Option<?>>> action : actions) {
            action.accept(resolvedOptions);
        }

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
