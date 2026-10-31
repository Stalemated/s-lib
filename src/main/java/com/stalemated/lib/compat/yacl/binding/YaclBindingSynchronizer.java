package com.stalemated.lib.compat.yacl.binding;

import com.stalemated.lib.config.session.ConfigSession;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Coordinates reactive state synchronization across YACL {@link Option} and {@link ListOption}
 * instances that show the same stored value of a {@link ConfigSession} within one screen.
 * <p>
 * Prevents stale duplicate widgets in multi-tab screens from overwriting changes during
 * YACL's two-pass {@code finishOrSave()} cycle, and normalizes pending widget values when
 * setters clamp or reformat input data. One instance lives as long as the screen that owns it.
 */
public final class YaclBindingSynchronizer {

    private final ConfigSession<?> session;
    private final Map<String, List<Option<?>>> optionsByKey = new HashMap<>();

    public YaclBindingSynchronizer(ConfigSession<?> session) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
    }

    @FunctionalInterface
    private interface BindingStep<B, V> {
        void bind(B builder, V def, Supplier<V> getter, Consumer<V> setter);
    }

    private <V, O extends Option<V>, B> O buildSynced(
            String key,
            B builder,
            V defaultValue,
            Supplier<V> getter,
            Consumer<V> setter,
            Consumer<B> postConfigurer,
            BindingStep<B, V> bindStep,
            Function<B, O> buildStep
    ) {
        AtomicReference<O> optRef = new AtomicReference<>();
        bindStep.bind(
                builder,
                defaultValue,
                getter,
                val -> applySyncedMutation(key, optRef.get(), () -> setter.accept(val))
        );
        if (postConfigurer != null) {
            postConfigurer.accept(builder);
        }
        O built = buildStep.apply(builder);
        optRef.set(built);
        optionsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(built);
        built.addEventListener((opt, event) -> {
            if (session.hasPreview()) {
                session.applyToPreview(() -> setter.accept(opt.pendingValue()));
            }
        });
        return built;
    }

    /**
     * Binds, configures, and builds a standard YACL {@link Option}, registering it for cross-tab
     * sync and post-setter normalization within this screen.
     *
     * @param key            The key of the stored value; widgets sharing it are kept in sync.
     * @param builder        The YACL option builder.
     * @param defaultValue   The default GUI value.
     * @param getter         The binding getter reading from the session draft.
     * @param setter         The binding setter writing to the session draft.
     * @param postConfigurer Callback to configure the controller and apply any user customizer before building.
     * @param <V>            The GUI value type of the option.
     * @return The built and registered {@link Option}.
     */
    public <V> Option<V> buildSyncedOption(
            String key,
            Option.Builder<V> builder,
            V defaultValue,
            Supplier<V> getter,
            Consumer<V> setter,
            Consumer<Option.Builder<V>> postConfigurer
    ) {
        return buildSynced(key, builder, defaultValue, getter, setter, postConfigurer,
                Option.Builder::binding, Option.Builder::build);
    }

    /**
     * Binds, configures, and builds a YACL {@link ListOption}, registering it for cross-tab
     * sync and post-setter normalization within this screen.
     *
     * @param key            The key of the stored value; widgets sharing it are kept in sync.
     * @param builder        The YACL list option builder.
     * @param defaultValue   The default GUI list value.
     * @param getter         The binding getter reading from the session draft.
     * @param setter         The binding setter writing to the session draft.
     * @param postConfigurer Callback to configure the controller, initial supplier, and user customizer before building.
     * @param <E>            The GUI element type of the list option.
     * @return The built and registered {@link ListOption}.
     */
    public <E> ListOption<E> buildSyncedListOption(
            String key,
            ListOption.Builder<E> builder,
            List<E> defaultValue,
            Supplier<List<E>> getter,
            Consumer<List<E>> setter,
            Consumer<ListOption.Builder<E>> postConfigurer
    ) {
        return buildSynced(key, builder, defaultValue, getter, setter, postConfigurer,
                ListOption.Builder::binding, ListOption.Builder::build);
    }

    private void applySyncedMutation(String key, Option<?> sourceOption, Runnable mutation) {
        List<Option<?>> unchangedSiblings = new ArrayList<>();
        for (Option<?> sibling : optionsByKey.getOrDefault(key, List.of())) {
            if (sibling != sourceOption && !sibling.changed()) {
                unchangedSiblings.add(sibling);
            }
        }

        mutation.run();

        if (sourceOption != null && sourceOption.changed()) {
            sourceOption.forgetPendingValue();
        }

        for (Option<?> sibling : unchangedSiblings) {
            if (sibling.changed()) {
                sibling.forgetPendingValue();
            }
        }
    }
}
