package com.stalemated.lib.compat.yacl.util;

import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.session.ConfigSession;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Coordinates reactive state synchronization across YACL {@link Option} and {@link ListOption}
 * instances bound to the same {@link ConfigSession} and {@link OptionInfo}.
 * <p>
 * Prevents stale duplicate widgets in multi-tab screens from overwriting changes during
 * YACL's two-pass {@code finishOrSave()} cycle, and normalizes pending widget values when
 * setters clamp or reformat input data.
 */
public final class YaclBindingSynchronizer {

    private static final Map<ConfigSession<?>, Map<String, List<Option<?>>>> SESSION_OPTIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private YaclBindingSynchronizer() {}

    @FunctionalInterface
    private interface BindingStep<B, V> {
        void bind(B builder, V def, Supplier<V> getter, Consumer<V> setter);
    }

    private static <V, O extends Option<V>, B> O buildSynced(
            ConfigSession<?> session,
            OptionInfo info,
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
                val -> applySyncedMutation(session, info, optRef.get(), () -> setter.accept(val))
        );
        if (postConfigurer != null) {
            postConfigurer.accept(builder);
        }
        O built = buildStep.apply(builder);
        optRef.set(built);
        registerOption(session, info, built);
        built.addEventListener((opt, event) -> {
            if (session != null && session.hasPreview()) {
                session.applyToPreview(() -> setter.accept(opt.pendingValue()));
            }
        });
        return built;
    }

    /**
     * Binds, configures, and builds a standard YACL {@link Option}, registering it for cross-tab
     * synchronization and post-setter normalization within the given {@link ConfigSession}.
     *
     * @param session        The configuration session holding the draft buffer.
     * @param info           The option metadata associated with this binding.
     * @param builder        The YACL option builder.
     * @param defaultValue   The default GUI value.
     * @param getter         The binding getter reading from the session draft.
     * @param setter         The binding setter writing to the session draft.
     * @param postConfigurer Callback to configure the controller and apply any user customizer before building.
     * @param <V>            The GUI value type of the option.
     * @return The built and registered {@link Option}.
     */
    public static <V> Option<V> buildSyncedOption(
            ConfigSession<?> session,
            OptionInfo info,
            Option.Builder<V> builder,
            V defaultValue,
            Supplier<V> getter,
            Consumer<V> setter,
            Consumer<Option.Builder<V>> postConfigurer
    ) {
        return buildSynced(session, info, builder, defaultValue, getter, setter, postConfigurer,
                Option.Builder::binding, Option.Builder::build);
    }

    /**
     * Binds, configures, and builds a YACL {@link ListOption}, registering it for cross-tab
     * synchronization and post-setter normalization within the given {@link ConfigSession}.
     *
     * @param session        The configuration session holding the draft buffer.
     * @param info           The option metadata associated with this binding.
     * @param builder        The YACL list option builder.
     * @param defaultValue   The default GUI list value.
     * @param getter         The binding getter reading from the session draft.
     * @param setter         The binding setter writing to the session draft.
     * @param postConfigurer Callback to configure the controller, initial supplier, and user customizer before building.
     * @param <E>            The GUI element type of the list option.
     * @return The built and registered {@link ListOption}.
     */
    public static <E> ListOption<E> buildSyncedListOption(
            ConfigSession<?> session,
            OptionInfo info,
            ListOption.Builder<E> builder,
            List<E> defaultValue,
            Supplier<List<E>> getter,
            Consumer<List<E>> setter,
            Consumer<ListOption.Builder<E>> postConfigurer
    ) {
        return buildSynced(session, info, builder, defaultValue, getter, setter, postConfigurer,
                ListOption.Builder::binding, ListOption.Builder::build);
    }

    private static void registerOption(ConfigSession<?> session, OptionInfo info, Option<?> option) {
        if (session == null || info == null || option == null) return;
        Map<String, List<Option<?>>> byKey = SESSION_OPTIONS.computeIfAbsent(
                session,
                k -> new ConcurrentHashMap<>()
        );
        byKey.computeIfAbsent(info.getKey(), k -> new CopyOnWriteArrayList<>()).add(option);
    }

    private static List<Option<?>> getRegisteredOptions(ConfigSession<?> session, OptionInfo info) {
        if (session == null || info == null) return Collections.emptyList();
        Map<String, List<Option<?>>> byKey = SESSION_OPTIONS.get(session);
        if (byKey == null) return Collections.emptyList();
        return byKey.getOrDefault(info.getKey(), Collections.emptyList());
    }

    private static void applySyncedMutation(
            ConfigSession<?> session,
            OptionInfo info,
            Option<?> sourceOption,
            Runnable mutation
    ) {
        List<Option<?>> siblings = getRegisteredOptions(session, info);
        List<Option<?>> unchangedSiblings = new ArrayList<>();
        for (Option<?> sibling : siblings) {
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
