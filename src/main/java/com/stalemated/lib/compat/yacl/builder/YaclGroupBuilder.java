package com.stalemated.lib.compat.yacl.builder;

import com.stalemated.lib.compat.yacl.factory.YaclContext;
import com.stalemated.lib.compat.yacl.factory.YaclOptionFactory;
import com.stalemated.lib.config.model.OptionInfo;
import dev.isxander.yacl3.api.LabelOption;
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
