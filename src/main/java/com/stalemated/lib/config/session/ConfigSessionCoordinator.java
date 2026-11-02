package com.stalemated.lib.config.session;

import com.stalemated.lib.config.LocalConfigManager;

import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.stalemated.lib.SLib.LOGGER;

/**
 * Manages multiple active sessions in a single GUI screen (e.g., Synced + Local tabs).
 * Consolidates the commit pipeline into a single runnable for any {@code .save(Runnable)} hook.
 */
public class ConfigSessionCoordinator {
    private final Map<LocalConfigManager<?>, ConfigSession<?>> sessions = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> ConfigSession<T> getOrCreate(LocalConfigManager<T> manager) {
        Objects.requireNonNull(manager, "manager cannot be null");
        return (ConfigSession<T>) sessions.computeIfAbsent(manager, ConfigSession::new);
    }

    public boolean hasAnyChanges() {
        for (ConfigSession<?> session : sessions.values()) {
            if (session.hasChanges()) return true;
        }
        return false;
    }

    public void commitAll() {
        for (ConfigSession<?> session : sessions.values()) {
            if (session.hasChanges()) {
                commitOrKeepPending(session);
            }
        }
    }

    private static void commitOrKeepPending(ConfigSession<?> session) {
        try {
            session.commit();
        } catch (UncheckedIOException e) {
            LOGGER.error("Kept the screen changes of '{}' pending because they could not be saved: {}", session.getManager().getName(), e.getMessage());
        }
    }

    public void resetAll() {
        for (ConfigSession<?> session : sessions.values()) {
            session.reset();
        }
    }

    public Map<LocalConfigManager<?>, ConfigSession<?>> getSessions() {
        return Collections.unmodifiableMap(sessions);
    }
}
