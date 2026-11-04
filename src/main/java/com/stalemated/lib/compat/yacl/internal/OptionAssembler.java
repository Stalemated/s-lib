package com.stalemated.lib.compat.yacl.internal;

import com.stalemated.lib.compat.yacl.controller.builder.SelectDropdownControllerBuilder;
import com.stalemated.lib.compat.yacl.internal.YaclBindingSynchronizer.ToggleCondition;
import com.stalemated.lib.compat.yacl.registry.YaclControllerRegistry;
import com.stalemated.lib.config.OptionInfo;
import com.stalemated.lib.config.annotation.EnabledIf;
import com.stalemated.lib.config.internal.ConfigSession;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class OptionAssembler {

    private final ConfigSession<?> session;
    private final YaclBindingSynchronizer synchronizer;
    private final YaclControllerRegistry registry;

    public OptionAssembler(ConfigSession<?> session, YaclBindingSynchronizer synchronizer, YaclControllerRegistry registry) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.synchronizer = Objects.requireNonNull(synchronizer, "synchronizer cannot be null");
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
    }

    public Option<?> option(YaclOptionMetadata meta, OptionInfo info, Consumer<Option.Builder<?>> customizer) {
        TypeLens<Object, Object> lens = this.lensFor(info.type(), meta).orElseThrow(() ->
                new UnsupportedOperationException("No YACL controller or adapter registered for type: "
                        + info.type().getName() + " on config option: " + info.getKey()));
        Option<?> built = option(meta, info, ValueAccessor.option(session, info), lens, info.defaultCopy(), customizer);
        applyEnabledIf(info, built);
        return built;
    }

    @SuppressWarnings("unchecked")
    public <E> ListOption<E> list(YaclOptionMetadata meta, OptionInfo info, Consumer<ListOption.Builder<E>> customizer) {
        Class<?> elementType = info.getCollectionElementType();
        TypeLens<Object, E> lens = this.<Object, E>lensFor(elementType, meta).orElseThrow(() ->
                new UnsupportedOperationException("No YACL controller or adapter registered for list element type: "
                        + elementType.getName() + " on config option: " + info.getKey()));

        OptionInfo elementInfo = info.getElementTemplate() instanceof OptionInfo template ? template : info;
        E initialElement = initialElement(elementInfo, elementType, lens);
        if (initialElement == null) {
            throw new UnsupportedOperationException("Cannot create ListOption for option '" + info.getKey()
                    + "' with element type " + elementType.getName()
                    + ": No initial value supplier registered. Register one via controllerRegistry.registerInitialSupplier.");
        }

        Collection<Object> modelDefault = (Collection<Object>) info.defaultCopy();
        ListOption<E> built = list(meta, elementInfo, ValueAccessor.collection(session, info), lens, modelDefault, initialElement, customizer);
        applyEnabledIf(info, built);
        return built;
    }

    public void applyEnabledIf(OptionInfo info, Option<?> built) {
        List<ToggleCondition> conditions = conditionsOf(info);
        if (conditions.isEmpty()) return;
        synchronizer.enableWhen(built, built.available(), conditions);
    }

    private List<ToggleCondition> conditionsOf(OptionInfo info) {
        List<ToggleCondition> conditions = new ArrayList<>();
        Set<String> chain = new LinkedHashSet<>(List.of(info.getKey()));
        OptionInfo dependent = info;
        Optional<EnabledIf> enabledIf = info.annotation(EnabledIf.class);
        while (enabledIf.isPresent()) {
            ToggleCondition condition = conditionOf(dependent, enabledIf.get());
            if (!chain.add(condition.toggle().getKey())) {
                throw new IllegalArgumentException("@EnabledIf on '" + info.getKey() + "' forms a cycle: " + String.join(" -> ", chain) + " -> " + condition.toggle().getKey());
            }
            conditions.add(condition);
            dependent = condition.toggle();
            enabledIf = dependent.annotation(EnabledIf.class);
        }
        return conditions;
    }

    private ToggleCondition conditionOf(OptionInfo dependent, EnabledIf enabledIf) {
        OptionInfo toggle = session.getOptionTree().get(enabledIf.value());
        if (toggle == null) {
            throw new IllegalArgumentException("@EnabledIf on '" + dependent.getKey() + "' names '" + enabledIf.value() + "', which is not an option of this config");
        }
        List<String> toggleValues = valuesOf(dependent, toggle);
        for (String value : enabledIf.equals()) {
            if (!toggleValues.contains(value)) {
                throw new IllegalArgumentException("@EnabledIf on '" + dependent.getKey() + "' lists '" + value + "', which is not a value of '" + toggle.getKey() + "': " + toggleValues);
            }
        }
        return new ToggleCondition(toggle, Set.copyOf(Arrays.asList(enabledIf.equals())));
    }

    private static List<String> valuesOf(OptionInfo dependent, OptionInfo toggle) {
        Class<?> type = toggle.type();
        if (type == boolean.class || type == Boolean.class) {
            return List.of("true", "false");
        }
        if (type.isEnum()) {
            return Arrays.stream(type.getEnumConstants()).map(constant -> ((Enum<?>) constant).name()).toList();
        }
        throw new IllegalArgumentException("@EnabledIf on '" + dependent.getKey() + "' needs a boolean or enum option, '" + toggle.getKey() + "' is a " + type.getSimpleName());
    }

    public <M, G> Optional<TypeLens<M, G>> lensFor(Class<?> type, YaclOptionMetadata meta) {
        Optional<TypeLens<M, G>> registered = registry.findLens(type);
        if (registered.isPresent() || type == null || !type.isEnum()) {
            return registered;
        }
        return Optional.of(enumLens(type, meta));
    }

    @SuppressWarnings("unchecked")
    public <E, G> G initialElement(OptionInfo elementInfo, Class<?> elementType, TypeLens<E, G> lens) {
        E initial = (E) elementInfo.clampValue(registry.resolveInitialValue(elementType));
        return lens.guiOf(initial, lens.fallback());
    }

    public <M, G> Option<G> option(
            YaclOptionMetadata meta,
            OptionInfo controllerInfo,
            ValueAccessor<M> value,
            TypeLens<M, G> lens,
            M modelDefault,
            Consumer<? super Option.Builder<G>> customizer
    ) {
        G guiDefault = lens.guiOf(modelDefault, lens.fallback());
        Option.Builder<G> builder = Option.<G>createBuilder()
                .name(meta.name())
                .description(meta.description())
                .available(meta.available());
        LastConversion<M, G> stored = new LastConversion<>(value::get, lens, guiDefault);

        return synchronizer.buildSyncedOption(
                value.slot(),
                builder,
                guiDefault,
                stored,
                stored::forget,
                gui -> {
                    M model = lens.fromGui().apply(gui);
                    if (model != null) {
                        value.set(model);
                    }
                },
                b -> {
                    b.controller(opt -> lens.controller().create(controllerInfo, opt));
                    if (customizer != null) {
                        customizer.accept(b);
                    }
                }
        );
    }

    public <E, G> ListOption<G> list(
            YaclOptionMetadata meta,
            OptionInfo elementInfo,
            ValueAccessor<List<E>> value,
            TypeLens<E, G> lens,
            Collection<E> modelDefault,
            G initialElement,
            Consumer<? super ListOption.Builder<G>> customizer
    ) {
        List<G> guiDefault = toGui(lens, modelDefault);
        ListOption.Builder<G> builder = ListOption.<G>createBuilder()
                .name(meta.name())
                .description(meta.description())
                .available(meta.available());

        return synchronizer.buildSyncedListOption(
                value.slot(),
                builder,
                guiDefault,
                () -> {
                    List<E> current = value.get();
                    return current != null ? toGui(lens, current) : new ArrayList<>(guiDefault);
                },
                guiList -> value.set(fromGui(lens, guiList)),
                b -> {
                    b.controller(opt -> lens.controller().create(elementInfo, opt));
                    b.initial(initialElement);
                    if (customizer != null) {
                        customizer.accept(b);
                    }
                }
        );
    }

    private static <E, G> List<G> toGui(TypeLens<E, G> lens, Collection<E> models) {
        List<G> guiList = new ArrayList<>();
        if (models == null) return guiList;
        for (E model : models) {
            G gui = lens.guiOf(model, null);
            if (gui != null) {
                guiList.add(gui);
            }
        }
        return guiList;
    }

    private static <E, G> List<E> fromGui(TypeLens<E, G> lens, List<G> guiList) {
        List<E> models = new ArrayList<>(guiList.size());
        for (G gui : guiList) {
            E model = gui != null ? lens.fromGui().apply(gui) : null;
            if (model != null) {
                models.add(model);
            }
        }
        return models;
    }

    private static final class LastConversion<M, G> implements Supplier<G> {
        private final Supplier<M> model;
        private final TypeLens<M, G> lens;
        private final G whenAbsent;
        private boolean converted;
        private Object lastModel;
        private G lastGui;

        private LastConversion(Supplier<M> model, TypeLens<M, G> lens, G whenAbsent) {
            this.model = model;
            this.lens = lens;
            this.whenAbsent = whenAbsent;
        }

        @Override
        public G get() {
            M current = model.get();
            if (converted && sameModel(current, lastModel)) {
                return lastGui;
            }
            lastGui = lens.guiOf(current, whenAbsent);
            lastModel = current instanceof Collection<?> collection ? new ArrayList<>(collection) : current;
            converted = true;
            return lastGui;
        }

        private void forget() {
            converted = false;
        }

        private static boolean sameModel(Object current, Object last) {
            if (!(current instanceof Collection<?> collection)) {
                return Objects.equals(current, last);
            }
            if (!(last instanceof List<?> snapshot) || snapshot.size() != collection.size()) {
                return false;
            }
            if (collection instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    if (!Objects.equals(list.get(i), snapshot.get(i))) return false;
                }
                return true;
            }
            int index = 0;
            for (Object element : collection) {
                if (!Objects.equals(element, snapshot.get(index++))) return false;
            }
            return true;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <M, G> TypeLens<M, G> enumLens(Class<?> type, YaclOptionMetadata meta) {
        return (TypeLens<M, G>) enumLensOf((Class) type, meta);
    }

    private static <E extends Enum<E>> TypeLens<E, E> enumLensOf(Class<E> type, YaclOptionMetadata meta) {
        E[] constants = type.getEnumConstants();
        E first = constants.length > 0 ? constants[0] : null;
        return TypeLens.identity(first, (info, opt) -> SelectDropdownControllerBuilder.createEnum(opt).formatValue(meta::formatEnum));
    }
}
