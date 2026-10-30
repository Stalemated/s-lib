package com.stalemated.lib.compat.yacl.factory;

import com.stalemated.lib.compat.yacl.registry.YaclControllerRegistry;
import com.stalemated.lib.compat.yacl.registry.YaclOptionMetadata;
import com.stalemated.lib.config.model.OptionInfo;
import dev.isxander.yacl3.api.ListOption;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Metadata-driven factory that produces YACL {@link ListOption} instances from {@link OptionInfo}
 * and a {@link YaclContext}.
 * <p>
 * Supports both direct element types (primitives, Color, String, Enum) and adapted element types
 * (e.g., TextColor, ResourceLocation, UUID, Pattern) through the Lens pattern.
 *
 * @param <T> The config model type.
 * @param context The shared YACL UI construction context.
 */
public record YaclListOptionFactory<T>(YaclContext<T> context) {

    public YaclListOptionFactory {
        Objects.requireNonNull(context, "context cannot be null");
    }

    public <E> ListOption<E> buildListOption(OptionInfo info) {
        return buildListOption(info, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <E> ListOption<E> buildListOption(OptionInfo info, Consumer<ListOption.Builder<E>> customizer) {
        YaclOptionMetadata meta = context.resolveMetadata(info);
        Class<?> elemClass = resolveElementType(info);

        // Check if the element type is adapted (e.g., TextColor, ResourceLocation, UUID, Pattern)
        YaclControllerRegistry.AdaptedType adaptedType = context.controllerRegistry().findAdaptedType(elemClass);
        if (adaptedType != null) {
            return (ListOption<E>) createAdaptedListOption(info, meta, adaptedType, (Consumer) customizer);
        }

        // Direct controller (e.g., Color, String, Integer, Enum)
        YaclControllerRegistry.ControllerFactory controllerFactory = context.controllerRegistry().findDirectController(elemClass);
        if (controllerFactory == null) {
            throw new UnsupportedOperationException("No YACL controller or adapter registered for list element type: "
                    + elemClass.getName() + " on config option: " + info.getKey());
        }

        ListOption.Builder<E> builder = ListOption.createBuilder();
        builder.name(meta.name())
                .description(meta.description())
                .available(meta.available());

        List<E> defaultVal = (List<E>) info.getDefaultValue();
        List<E> defaultCopy = defaultVal != null ? new ArrayList<>(defaultVal) : new ArrayList<>();

        Supplier<E> initialSupplier = () -> context.controllerRegistry().resolveInitialValue(elemClass);
        E initialVal = initialSupplier.get();
        if (initialVal == null) {
            throw new UnsupportedOperationException("Cannot create ListOption for option '" + info.getKey()
                    + "' with element type " + elemClass.getName()
                    + ": No initial value supplier registered. Register one via controllerRegistry.registerInitialSupplier.");
        }

        return context.synchronizer().buildSyncedListOption(
                info,
                builder,
                defaultCopy,
                () -> {
                    Object raw = context.session().get(info);
                    if (raw instanceof Collection<?> coll) {
                        return new ArrayList<>((Collection<E>) coll);
                    }
                    return new ArrayList<>();
                },
                list -> persistCollection(info, list),
                b -> {
                    b.controller(opt -> controllerFactory.create(info, opt));
                    b.initial(initialSupplier);
                    if (customizer != null) customizer.accept(b);
                }
        );
    }

    @SuppressWarnings({"unchecked"})
    private <M, G> ListOption<G> createAdaptedListOption(
            OptionInfo info,
            YaclOptionMetadata meta,
            YaclControllerRegistry.AdaptedType<M, G> adaptedType,
            Consumer<ListOption.Builder<G>> customizer
    ) {
        ListOption.Builder<G> builder = ListOption.createBuilder();
        builder.name(meta.name())
                .description(meta.description())
                .available(meta.available());

        List<M> modelDefault = (List<M>) info.getDefaultValue();
        List<G> guiDefault = new ArrayList<>();
        if (modelDefault != null) {
            for (M item : modelDefault) {
                if (item != null) guiDefault.add(adaptedType.toGui().apply(item));
            }
        }

        return context.synchronizer().buildSyncedListOption(
                info,
                builder,
                guiDefault,
                () -> {
                    List<M> modelList = context.session().get(info);
                    if (modelList == null) return new ArrayList<>();
                    List<G> guiList = new ArrayList<>();
                    for (M item : modelList) {
                        if (item != null) guiList.add(adaptedType.toGui().apply(item));
                    }
                    return guiList;
                },
                guiList -> {
                    List<M> modelList = new ArrayList<>();
                    for (G guiItem : guiList) {
                        if (guiItem != null) {
                            M modelItem = adaptedType.fromGui().apply(guiItem);
                            if (modelItem != null) modelList.add(modelItem);
                        }
                    }
                    persistCollection(info, modelList);
                },
                b -> {
                    b.controller(opt -> adaptedType.guiControllerFactory().create(info, opt));
                    b.initial(() -> {
                        M initialModel = context.controllerRegistry().resolveInitialValue(adaptedType.modelType());
                        if (initialModel != null) {
                            G initialGui = adaptedType.toGui().apply(initialModel);
                            if (initialGui != null) return initialGui;
                        }
                        return adaptedType.fallbackGuiDefault();
                    });
                    if (customizer != null) customizer.accept(b);
                }
        );
    }

    private void persistCollection(OptionInfo info, Collection<?> collection) {
        if (info.type() != null && Set.class.isAssignableFrom(info.type())) {
            context.session().set(info, new LinkedHashSet<>(collection));
        } else {
            context.session().set(info, new ArrayList<>(collection));
        }
    }

    public static Class<?> resolveElementType(OptionInfo info) {
        return info.getCollectionElementType();
    }
}
