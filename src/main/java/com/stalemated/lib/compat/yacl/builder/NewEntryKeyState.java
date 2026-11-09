package com.stalemated.lib.compat.yacl.builder;

import dev.isxander.yacl3.api.StateManager;

final class NewEntryKeyState implements StateManager<String> {
    private String typedKey = "";
    private StateListener<String> listener = StateListener.noop();

    @Override
    public void set(String value) {
        String previous = typedKey;
        typedKey = value;
        if (!previous.equals(value)) {
            listener.onStateChange(previous, value);
        }
    }

    @Override
    public String get() {
        return typedKey;
    }

    @Override
    public void apply() {
    }

    @Override
    public void resetToDefault(ResetAction action) {
        set("");
    }

    @Override
    public void sync() {
        set("");
    }

    @Override
    public boolean isSynced() {
        return true;
    }

    @Override
    public boolean isDefault() {
        return typedKey.isEmpty();
    }

    @Override
    public void addListener(StateListener<String> stateListener) {
        listener = listener.andThen(stateListener);
    }
}
