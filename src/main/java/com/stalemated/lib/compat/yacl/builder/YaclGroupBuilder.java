package com.stalemated.lib.compat.yacl.builder;

import com.stalemated.lib.compat.yacl.factory.YaclContext;
import com.stalemated.lib.compat.yacl.factory.YaclOptionFactory;
import com.stalemated.lib.config.model.OptionInfo;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/**
 * Fluent builder for YACL {@link OptionGroup}.
 * Groups individual options together under a titled and collapsible group.
 *
 * @param <T> The config model type.
 */
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

    @SuppressWarnings("UnusedReturnValue")
    public YaclGroupBuilder<T> name(Component name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclGroupBuilder<T> description(OptionDescription description) {
        this.description = description;
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclGroupBuilder<T> description(Component description) {
        this.description = OptionDescription.of(description);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclGroupBuilder<T> collapsed(boolean collapsed) {
        this.collapsed = collapsed;
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclGroupBuilder<T> option(Option<?> customOption) {
        this.actions.add(list -> list.add(customOption));
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclGroupBuilder<T> configureOption(String key, Consumer<Option.Builder<?>> customizer) {
        this.overrides.put(key, customizer);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
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
                Consumer<Option.Builder<?>> customizer = overrides.get(key);
                list.add(optionFactory.buildOption(info, customizer));
            }
        });
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public YaclGroupBuilder<T> includePrefix(String prefix) {
        String cleanPrefix = prefix.endsWith(".") ? prefix : prefix + ".";
        actions.add(list -> {
            for (OptionInfo info : context.session().getOptionTree().all()) {
                if (info.getKey().startsWith(cleanPrefix) || info.getKey().equals(prefix)) {
                    if (info.isCollection() || info.isMap()) {
                        continue;
                    }
                    Consumer<Option.Builder<?>> customizer = overrides.get(info.getKey());
                    list.add(optionFactory.buildOption(info, customizer));
                }
            }
        });
        return this;
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
