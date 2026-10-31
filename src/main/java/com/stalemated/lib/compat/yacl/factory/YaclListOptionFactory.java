package com.stalemated.lib.compat.yacl.factory;

import com.stalemated.lib.compat.yacl.binding.OptionAssembler;
import com.stalemated.lib.compat.yacl.binding.ValueAccessor;
import com.stalemated.lib.compat.yacl.registry.TypeLens;
import com.stalemated.lib.compat.yacl.registry.YaclOptionMetadata;
import com.stalemated.lib.config.model.OptionInfo;
import dev.isxander.yacl3.api.ListOption;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

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

    @SuppressWarnings("unchecked")
    public <E> ListOption<E> buildListOption(OptionInfo info, Consumer<ListOption.Builder<E>> customizer) {
        YaclOptionMetadata meta = context.resolveMetadata(info);
        Class<?> elemClass = resolveElementType(info);
        OptionAssembler assembler = context.assembler();

        TypeLens<Object, E> lens = assembler.<Object, E>lensFor(elemClass, meta).orElseThrow(() ->
                new UnsupportedOperationException("No YACL controller or adapter registered for list element type: "
                        + elemClass.getName() + " on config option: " + info.getKey()));

        E initialElement = assembler.initialElement(elemClass, lens);
        if (initialElement == null) {
            throw new UnsupportedOperationException("Cannot create ListOption for option '" + info.getKey()
                    + "' with element type " + elemClass.getName()
                    + ": No initial value supplier registered. Register one via controllerRegistry.registerInitialSupplier.");
        }

        OptionInfo elementInfo = info.getElementTemplate() instanceof OptionInfo template ? template : info;
        Collection<Object> modelDefault = (Collection<Object>) info.defaultCopy();
        return assembler.list(meta, elementInfo, ValueAccessor.collection(context.session(), info), lens, modelDefault, initialElement, customizer);
    }

    public static Class<?> resolveElementType(OptionInfo info) {
        return info.getCollectionElementType();
    }
}
