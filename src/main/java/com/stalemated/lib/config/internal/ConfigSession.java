package com.stalemated.lib.config.internal;

import com.stalemated.lib.config.ConfigManager;
import com.stalemated.lib.config.OptionInfo;
import com.stalemated.lib.config.OptionTree;

import java.util.*;
import java.util.function.Consumer;

/**
 * Isolated in-memory transaction buffer for a config model.
 * Buffers all interactive UI changes and commits them in a single atomic modification,
 * while optionally routing live pending widget updates to a separate preview instance.
 *
 * @param <T> The config model type.
 */
public class ConfigSession<T> {
    private final ConfigManager<T> manager;
    private final OptionTree optionTree;
    private final T base;
    private final T draft;
    private T preview;
    private Consumer<T> previewListener;
    private boolean applyingToPreview = false;

    public ConfigSession(ConfigManager<T> manager) {
        this.manager = Objects.requireNonNull(manager, "manager cannot be null");
        this.optionTree = Objects.requireNonNull(manager.getOptionTree(), "optionTree cannot be null");
        this.base = manager.cloneConfig(manager.get());
        this.draft = manager.cloneConfig(base);
    }

    private T activeTarget() {
        return (applyingToPreview && preview != null) ? preview : draft;
    }

    public T getDraft() {
        return draft;
    }

    /**
     * Binds a live preview instance that is automatically updated whenever any YACL option
     * bound to this session changes its pending value, without mutating the session draft.
     *
     * @param preview         The live preview instance to mutate on pending UI changes.
     * @param previewListener Optional callback invoked after each preview update (e.g., cache invalidation).
     */
    public void bindPreview(T preview, Consumer<T> previewListener) {
        this.preview = preview;
        this.previewListener = previewListener;
    }

    public boolean hasPreview() {
        return preview != null;
    }

    /**
     * Temporarily routes session reads and writes to the bound preview instance for the duration
     * of {@code mutation}, then notifies the preview listener if present.
     *
     * @param mutation The mutation closure (typically an option binding setter).
     */
    public void applyToPreview(Runnable mutation) {
        if (preview == null || mutation == null) return;
        boolean prev = this.applyingToPreview;
        this.applyingToPreview = true;

        try {
            mutation.run();
            if (previewListener != null) previewListener.accept(preview);
        } finally {
            this.applyingToPreview = prev;
        }
    }

    public ConfigManager<T> getManager() {
        return manager;
    }

    public OptionTree getOptionTree() {
        return optionTree;
    }

    @SuppressWarnings("unchecked")
    public <V> V get(OptionInfo option) {
        Objects.requireNonNull(option, "option cannot be null");
        return (V) option.getValue(activeTarget());
    }

    public <V> void set(OptionInfo option, V value) {
        Objects.requireNonNull(option, "option cannot be null");
        option.setValue(activeTarget(), value);
    }

    public boolean hasChanges() {
        return !optionTree.findChanges(base, draft).isEmpty();
    }

    /**
     * Commits the options edited in this session into the active manager via a single atomic call.
     * Options the session did not touch keep whatever value they have now, even if it changed
     * after the session was opened. Edits the manager rejects stay pending in the session.
     */
    public void commit() {
        List<OptionChange> edits = optionTree.findChanges(base, draft);
        if (edits.isEmpty()) return;

        Map<OptionInfo, Object> values = new LinkedHashMap<>();
        for (OptionChange edit : edits) {
            OptionInfo option = edit.option();
            values.put(option, optionTree.deepCopy().of(edit.clampedValue(), option.genericType()));
        }
        manager.modifyOptions(values);
        advanceBaseToAppliedEdits(edits);
    }

    private void advanceBaseToAppliedEdits(List<OptionChange> edits) {
        T active = manager.get();
        for (OptionChange edit : edits) {
            OptionInfo option = edit.option();
            Object applied = option.getValue(active);

            if (optionTree.sameValue(applied, edit.clampedValue())) {
                option.setValue(base, optionTree.deepCopy().of(applied, option.genericType()));
            }
        }
    }
}
