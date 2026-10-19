package com.stalemated.lib.config.session;

import com.stalemated.lib.config.manager.LocalConfigManager;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;

import java.util.*;

/**
 * Isolated in-memory transaction buffer for a config model.
 * Buffers all interactive UI changes and commits them in a single atomic modification.
 *
 * @param <T> The config model type.
 */
public class ConfigSession<T> {
    private final LocalConfigManager<T> manager;
    private final OptionTree optionTree;
    private T draft;
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

    public T getDraft() {
        return draft;
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
        return (V) option.getValue(draft);
    }

    public <V> void set(OptionInfo option, V value) {
        Objects.requireNonNull(option, "option cannot be null");
        option.setValue(draft, value);
        this.committed = false;
    }

    @SuppressWarnings("unchecked")
    public <V> V get(String key) {
        OptionInfo option = optionTree.get(key);
        if (option == null) {
            throw new IllegalArgumentException("Unknown config option: " + key);
        }
        return (V) option.getValue(draft);
    }

    public <V> void set(String key, V value) {
        OptionInfo option = optionTree.get(key);
        if (option == null) {
            throw new IllegalArgumentException("Unknown config option: " + key);
        }
        option.setValue(draft, value);
        this.committed = false;
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
