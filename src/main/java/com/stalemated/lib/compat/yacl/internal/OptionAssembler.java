package com.stalemated.lib.compat.yacl.internal;

import com.stalemated.lib.compat.yacl.controller.builder.SelectDropdownControllerBuilder;
import com.stalemated.lib.compat.yacl.registry.YaclControllerRegistry;
import com.stalemated.lib.config.OptionInfo;
import com.stalemated.lib.config.internal.ConfigSession;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

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
        return option(meta, info, ValueAccessor.option(session, info), lens, info.defaultCopy(), customizer);
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
        return list(meta, elementInfo, ValueAccessor.collection(session, info), lens, modelDefault, initialElement, customizer);
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

        return synchronizer.buildSyncedOption(
                value.slot(),
                builder,
                guiDefault,
                () -> lens.guiOf(value.get(), guiDefault),
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
