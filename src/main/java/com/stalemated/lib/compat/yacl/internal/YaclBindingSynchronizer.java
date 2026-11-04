package com.stalemated.lib.compat.yacl.internal;

import com.stalemated.lib.config.internal.ConfigSession;
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
    private final Map<String, List<SlottedOption>> optionsByContainer = new HashMap<>();

    private record SlottedOption(SyncSlot slot, Option<?> option, Runnable forgetCachedValue) {}

    public YaclBindingSynchronizer(ConfigSession<?> session) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
    }

    @FunctionalInterface
    private interface BindingStep<B, V> {
        void bind(B builder, V def, Supplier<V> getter, Consumer<V> setter);
    }

    private <V, O extends Option<V>, B> O buildSynced(
            SyncSlot slot,
            B builder,
            V defaultValue,
            Supplier<V> getter,
            Runnable forgetCachedValue,
            Consumer<V> setter,
            Consumer<B> postConfigurer,
            BindingStep<B, V> bindStep,
            Function<B, O> buildStep
    ) {
        AtomicReference<O> optRef = new AtomicReference<>();
        if (postConfigurer != null) {
            postConfigurer.accept(builder);
        }
        bindStep.bind(
                builder,
                defaultValue,
                getter,
                val -> applySyncedMutation(slot, optRef.get(), () -> setter.accept(val))
        );
        O built = buildStep.apply(builder);
        optRef.set(built);
        optionsByContainer.computeIfAbsent(slot.container(), container -> new ArrayList<>()).add(new SlottedOption(slot, built, forgetCachedValue));
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
     * @param slot           The stored value the option shows; widgets with overlapping slots are kept in sync.
     * @param builder        The YACL option builder.
     * @param defaultValue   The default GUI value.
     * @param getter         The binding getter reading from the session draft.
     * @param forgetCachedValue Drops what the getter cached; run after every write to an overlapping slot,
     *                       since a write can change the stored value in place.
     * @param setter         The binding setter writing to the session draft.
     * @param postConfigurer Callback to configure the controller and apply any user customizer before the
     *                       synced binding is set, so a customizer cannot replace it.
     * @param <V>            The GUI value type of the option.
     * @return The built and registered {@link Option}.
     */
    public <V> Option<V> buildSyncedOption(
            SyncSlot slot,
            Option.Builder<V> builder,
            V defaultValue,
            Supplier<V> getter,
            Runnable forgetCachedValue,
            Consumer<V> setter,
            Consumer<Option.Builder<V>> postConfigurer
    ) {
        return buildSynced(slot, builder, defaultValue, getter, forgetCachedValue, setter, postConfigurer,
                Option.Builder::binding, Option.Builder::build);
    }

    /**
     * Binds, configures, and builds a YACL {@link ListOption}, registering it for cross-tab
     * sync and post-setter normalization within this screen.
     *
     * @param slot           The stored value the option shows; widgets with overlapping slots are kept in sync.
     * @param builder        The YACL list option builder.
     * @param defaultValue   The default GUI list value.
     * @param getter         The binding getter reading from the session draft.
     * @param setter         The binding setter writing to the session draft.
     * @param postConfigurer Callback to configure the controller, initial supplier, and user customizer before the
     *                       synced binding is set, so a customizer cannot replace it.
     * @param <E>            The GUI element type of the list option.
     * @return The built and registered {@link ListOption}.
     */
    public <E> ListOption<E> buildSyncedListOption(
            SyncSlot slot,
            ListOption.Builder<E> builder,
            List<E> defaultValue,
            Supplier<List<E>> getter,
            Consumer<List<E>> setter,
            Consumer<ListOption.Builder<E>> postConfigurer
    ) {
        return buildSynced(slot, builder, defaultValue, getter, () -> {}, setter, postConfigurer,
                ListOption.Builder::binding, ListOption.Builder::build);
    }

    private void applySyncedMutation(SyncSlot slot, Option<?> sourceOption, Runnable mutation) {
        List<SlottedOption> overlapping = new ArrayList<>();
        List<Option<?>> unchangedSiblings = new ArrayList<>();
        for (SlottedOption sibling : optionsByContainer.getOrDefault(slot.container(), List.of())) {
            if (!sibling.slot().overlaps(slot)) continue;
            overlapping.add(sibling);
            if (sibling.option() != sourceOption && !sibling.option().changed()) {
                unchangedSiblings.add(sibling.option());
            }
        }

        mutation.run();
        overlapping.forEach(written -> written.forgetCachedValue().run());

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
