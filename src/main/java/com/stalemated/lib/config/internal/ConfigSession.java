package com.stalemated.lib.config.internal;

import com.stalemated.lib.config.ConfigManager;
import com.stalemated.lib.config.OptionInfo;
import com.stalemated.lib.config.OptionTree;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.stalemated.lib.SLib.LOGGER;

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

    public boolean isBehindManager() {
        return !optionTree.findChanges(base, manager.get()).isEmpty();
    }

    public boolean catchUpWithManager() {
        T current = manager.get();
        List<OptionChange> moved = optionTree.findChanges(base, current);
        for (OptionChange change : moved) {
            OptionInfo option = change.option();
            Object before = option.getValue(base);
            Object edited = option.getValue(draft);
            Object now = option.getValue(current);
            Object caughtUp = optionTree.sameValue(before, edited) ? now : mergeWithCurrent(option, before, edited, now);
            option.setValue(draft, optionTree.deepCopy().of(caughtUp, option.genericType()));
            option.setValue(base, optionTree.deepCopy().of(now, option.genericType()));
        }
        return !moved.isEmpty();
    }

    public void discardChanges() {
        optionTree.copyAllValues(base, draft);
    }

    public void discardChanges(Predicate<OptionInfo> options) {
        for (OptionInfo option : optionTree.all()) {
            if (options.test(option)) {
                option.setValue(draft, optionTree.deepCopy().of(option.getValue(base), option.genericType()));
            }
        }
    }

    public void syncPreviewWithDraft() {
        if (preview == null) return;
        optionTree.copyAllValues(draft, preview);
        if (previewListener != null) previewListener.accept(preview);
    }

    /**
     * Commits the options edited in this session into the active manager via a single atomic call.
     * Options the session did not touch keep whatever value they have now, even if it changed
     * after the session was opened. Edits the manager rejects stay pending in the session.
     */
    public void commit() {
        List<OptionChange> edits = optionTree.findChanges(base, draft);
        if (edits.isEmpty()) return;

        T current = manager.get();
        Map<OptionInfo, Object> values = new LinkedHashMap<>();
        for (OptionChange edit : edits) {
            OptionInfo option = edit.option();
            Object merged = mergeWithCurrent(option, option.getValue(base), edit.clampedValue(), option.getValue(current));
            values.put(option, optionTree.deepCopy().of(merged, option.genericType()));
        }
        manager.modifyOptions(values);
        advanceBaseToAppliedEdits(edits, values);
    }

    private Object mergeWithCurrent(OptionInfo option, Object before, Object edited, Object current) {
        if (optionTree.sameValue(before, current)) return edited;
        if (before instanceof List<?> beforeList && edited instanceof List<?> editedList && current instanceof List<?> currentList) {
            return mergeLists(option, beforeList, editedList, currentList);
        }
        if (before instanceof Map<?, ?> beforeMap && edited instanceof Map<?, ?> editedMap && current instanceof Map<?, ?> currentMap) {
            return mergeMaps(beforeMap, editedMap, currentMap);
        }
        return edited;
    }

    private List<?> mergeLists(OptionInfo option, List<?> before, List<?> edited, List<?> current) {
        if (edited.size() != before.size() || current.size() < before.size()) {
            LOGGER.warn("The screen kept its own '{}' over the changes made to it elsewhere while it was open: it had {} elements, the screen has {} and it had become {}",
                    option.getKey(), before.size(), edited.size(), current.size());
            return edited;
        }
        List<Object> merged = new ArrayList<>(current);
        for (int i = 0; i < before.size(); i++) {
            if (!optionTree.sameValue(before.get(i), edited.get(i))) {
                merged.set(i, edited.get(i));
            }
        }
        return merged;
    }

    private Map<?, ?> mergeMaps(Map<?, ?> before, Map<?, ?> edited, Map<?, ?> current) {
        Map<Object, Object> merged = new LinkedHashMap<>(current);
        before.keySet().stream().filter(key -> !edited.containsKey(key)).forEach(merged::remove);
        for (Map.Entry<?, ?> entry : edited.entrySet()) {
            boolean editedHere = !before.containsKey(entry.getKey()) || !optionTree.sameValue(before.get(entry.getKey()), entry.getValue());
            if (editedHere) {
                merged.put(entry.getKey(), entry.getValue());
            }
        }
        return merged;
    }

    private void advanceBaseToAppliedEdits(List<OptionChange> edits, Map<OptionInfo, Object> sent) {
        T active = manager.get();
        for (OptionChange edit : edits) {
            OptionInfo option = edit.option();

            if (optionTree.sameValue(option.getValue(active), sent.get(option))) {
                option.setValue(base, optionTree.deepCopy().of(edit.clampedValue(), option.genericType()));
            }
        }
    }
}
