package com.stalemated.lib.config.session;

import com.stalemated.lib.config.LocalConfigManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
                session.commit();
            }
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
