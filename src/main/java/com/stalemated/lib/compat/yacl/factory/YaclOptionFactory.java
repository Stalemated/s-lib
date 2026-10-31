package com.stalemated.lib.compat.yacl.factory;

import com.stalemated.lib.compat.yacl.binding.OptionAssembler;
import com.stalemated.lib.compat.yacl.binding.ValueAccessor;
import com.stalemated.lib.compat.yacl.registry.TypeLens;
import com.stalemated.lib.compat.yacl.registry.YaclOptionMetadata;
import com.stalemated.lib.compat.yacl.util.NestedAccessor;
import com.stalemated.lib.config.model.OptionInfo;
import dev.isxander.yacl3.api.Option;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Concise facade factory for constructing top-level YACL {@link Option} instances and delegating
 * nested POJO field options to {@link YaclPojoFieldBuilder}.
 *
 * @param <T> The config model type.
 * @param context The shared YACL UI construction context.
 */
public record YaclOptionFactory<T>(YaclContext<T> context) {

    public YaclOptionFactory {
        Objects.requireNonNull(context, "context cannot be null");
    }

    public Option<?> buildOption(OptionInfo optionInfo) {
        return buildOption(optionInfo, null);
    }

    public Option<?> buildOption(OptionInfo optionInfo, Consumer<Option.Builder<?>> customizer) {
        YaclOptionMetadata meta = context.resolveMetadata(optionInfo);
        OptionAssembler assembler = context.assembler();
        TypeLens<Object, Object> lens = assembler.lensFor(optionInfo.type(), meta).orElseThrow(() ->
                new UnsupportedOperationException("No YACL controller or adapter registered for type: "
                        + optionInfo.type().getName() + " on config option: " + optionInfo.getKey()));
        return assembler.option(meta, optionInfo, ValueAccessor.option(context.session(), optionInfo), lens, optionInfo.defaultCopy(), customizer);
    }

    public Option<?> buildPojoFieldOption(
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            Object sampleItem,
            boolean available
    ) {
        return YaclPojoFieldBuilder.buildPojoFieldOption(context, mapInfo, entryKey, field, sampleItem, available);
    }

    public Option<?> buildPojoFieldOption(
            OptionInfo listInfo,
            int index,
            Field field,
            Object sampleItem,
            boolean available
    ) {
        return YaclPojoFieldBuilder.buildPojoFieldOption(context, listInfo, index, field, sampleItem, available);
    }

    public Option<?> buildPojoFieldOption(
            OptionInfo containerInfo,
            NestedAccessor accessor,
            Field field,
            Object sampleItem,
            boolean available
    ) {
        return YaclPojoFieldBuilder.buildPojoFieldOption(context, containerInfo, accessor, field, sampleItem, available);
    }
}
