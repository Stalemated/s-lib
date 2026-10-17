package com.stalemated.lib.config.model;

import com.stalemated.lib.config.network.SyncMode;

/**
 * Immutable record representing a detected and clamped option value change during a transaction.
 *
 * @param option The option metadata descriptor.
 * @param clampedValue The new validated and clamped value.
 */
public record OptionChange(OptionInfo option, Object clampedValue) {

    /**
     * @return The synchronization mode of the underlying option.
     */
    public SyncMode syncMode() {
        return option.getSyncMode();
    }
}
