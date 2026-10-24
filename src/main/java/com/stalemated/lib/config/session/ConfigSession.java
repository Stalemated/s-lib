package com.stalemated.lib.config.session;

import com.stalemated.lib.config.manager.LocalConfigManager;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;

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
    private final LocalConfigManager<T> manager;
    private final OptionTree optionTree;
    private T draft;
    private T preview;
    private Consumer<T> previewListener;
    private boolean applyingToPreview = false;
    private boolean committed = false;

    public ConfigSession(LocalConfigManager<T> manager) {
        this.manager = Objects.requireNonNull(manager, "manager cannot be null");
        this.optionTree = Objects.requireNonNull(manager.getOptionTree(), "optionTree cannot be null");
        this.draft = createDraft();
    }

    private T createDraft() {
        T active = manager.getActiveConfig();
        if (active == null) return null;
        return manager.cloneConfig(active);
    }

    private T activeTarget() {
        return (applyingToPreview && preview != null) ? preview : draft;
    }

    public T getDraft() {
        return draft;
    }

    public T getPreview() {
        return preview;
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
    public synchronized void applyToPreview(Runnable mutation) {
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

    public LocalConfigManager<T> getManager() {
        return manager;
    }

    public OptionTree getOptionTree() {
        return optionTree;
    }

    public boolean isCommitted() {
        return committed && !hasChanges();
    }

    @SuppressWarnings("unchecked")
    public <V> V get(OptionInfo option) {
        Objects.requireNonNull(option, "option cannot be null");
        return (V) option.getValue(activeTarget());
    }

    public <V> void set(OptionInfo option, V value) {
        Objects.requireNonNull(option, "option cannot be null");
        option.setValue(activeTarget(), value);
        if (!applyingToPreview) this.committed = false;
    }

    @SuppressWarnings("unchecked")
    public <V> V get(String key) {
        OptionInfo option = optionTree.get(key);
        if (option == null) {
            throw new IllegalArgumentException("Unknown config option: " + key);
        }
        return (V) option.getValue(activeTarget());
    }

    public <V> void set(String key, V value) {
        OptionInfo option = optionTree.get(key);
        if (option == null) {
            throw new IllegalArgumentException("Unknown config option: " + key);
        }
        option.setValue(activeTarget(), value);
        if (!applyingToPreview) this.committed = false;
    }

    public boolean hasChanges() {
        if (draft == null || manager.getActiveConfig() == null) return false;
        return !optionTree.findChanges(manager.getActiveConfig(), draft).isEmpty();
    }

    public void reset() {
        this.draft = createDraft();
        this.committed = false;
    }

    /**
     * Commits all buffered modifications into the active manager via a single atomic call.
     */
    public synchronized void commit() {
        if (!hasChanges()) {
            committed = true;
            return;
        }

        manager.modify(workingCopy -> optionTree.copyAllValues(draft, workingCopy));
        committed = true;
    }
}
