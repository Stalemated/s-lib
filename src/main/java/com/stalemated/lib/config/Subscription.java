package com.stalemated.lib.config;

/**
 * Removes a listener added with {@link ConfigManager#onChange} or
 * {@link SyncedConfigManager#onViewChange}.
 */
@FunctionalInterface
public interface Subscription {

    /**
     * Removes the listener. It is safe from any thread, even while the listeners are running.
     * <p>
     * Calling it again does nothing, unless the same listener object was added more than once:
     * then each call removes one of them.
     */
    void unsubscribe();
}
